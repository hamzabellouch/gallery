package com.tkno.gallery.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer

object VideoEditorUtils {

    suspend fun extractVideoThumbnails(
        context: Context,
        videoUri: Uri,
        durationMs: Long,
        frameCount: Int = 8
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val actualDurationMs = if (durationMs > 0) {
                durationMs
            } else {
                val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durStr?.toLongOrNull() ?: 1000L
            }

            val stepUs = (actualDurationMs * 1000L) / frameCount.coerceAtLeast(1)
            for (i in 0 until frameCount) {
                val timeUs = (i * stepUs).coerceAtMost(actualDurationMs * 1000L)
                val frame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    retriever.getScaledFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        120,
                        120
                    )
                } else {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { original ->
                        val scaled = Bitmap.createScaledBitmap(original, 120, 120, true)
                        if (scaled != original) original.recycle()
                        scaled
                    }
                }
                if (frame != null) {
                    bitmaps.add(frame)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        bitmaps
    }

    suspend fun trimVideo(
        context: Context,
        sourceUri: Uri,
        originalName: String,
        startMs: Long,
        endMs: Long,
        rotationAngle: Float = 0f
    ): Uri? = withContext(Dispatchers.IO) {
        var tempOutputFile: File? = null
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            val pfd = context.contentResolver.openFileDescriptor(sourceUri, "r") ?: return@withContext null
            extractor.setDataSource(pfd.fileDescriptor)

            tempOutputFile = File(context.cacheDir, "trimmed_${System.currentTimeMillis()}.mp4")
            muxer = MediaMuxer(tempOutputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val trackCount = extractor.trackCount
            val trackMap = HashMap<Int, Int>(trackCount)
            var bufferSize = 1024 * 1024 // 1MB default

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""

                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    val muxerTrackIndex = muxer.addTrack(format)
                    trackMap[i] = muxerTrackIndex

                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        val newSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                        if (newSize > bufferSize) {
                            bufferSize = newSize
                        }
                    }
                }
            }

            if (trackMap.isEmpty()) {
                pfd.close()
                return@withContext null
            }

            // Apply orientation hint if rotated
            val retriever = MediaMetadataRetriever()
            var baseRotation = 0
            try {
                retriever.setDataSource(pfd.fileDescriptor)
                val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                baseRotation = rotationStr?.toIntOrNull() ?: 0
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { retriever.release() } catch (e: Exception) {}
            }

            val finalRotation = ((baseRotation + rotationAngle.toInt()) % 360 + 360) % 360
            muxer.setOrientationHint(finalRotation)

            muxer.start()

            val startUs = (startMs * 1000L).coerceAtLeast(0L)
            val endUs = if (endMs > startMs) endMs * 1000L else Long.MAX_VALUE

            // Seek extractor to startUs
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val buffer = ByteBuffer.allocateDirect(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)

                if (bufferInfo.size < 0) {
                    break
                }

                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags

                val trackIndex = extractor.sampleTrackIndex

                if (bufferInfo.presentationTimeUs > endUs) {
                    // Reached end of trimmed range
                    break
                }

                if (bufferInfo.presentationTimeUs >= startUs) {
                    val muxerTrackIndex = trackMap[trackIndex]
                    if (muxerTrackIndex != null) {
                        muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                    }
                }

                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            muxer = null
            extractor.release()
            pfd.close()

            // Save temp file to MediaStore
            val baseName = originalName.substringBeforeLast(".")
            val newFileName = "${baseName}_trimmed_${System.currentTimeMillis()}.mp4"

            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, newFileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Gallery")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val resultUri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (resultUri != null && tempOutputFile.exists()) {
                context.contentResolver.openOutputStream(resultUri)?.use { os ->
                    FileInputStream(tempOutputFile).use { fis ->
                        fis.copyTo(os)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    context.contentResolver.update(resultUri, contentValues, null, null)
                }
            }

            tempOutputFile.delete()
            resultUri
        } catch (e: Exception) {
            e.printStackTrace()
            try { muxer?.release() } catch (ex: Exception) {}
            try { extractor.release() } catch (ex: Exception) {}
            tempOutputFile?.delete()
            null
        }
    }
}
