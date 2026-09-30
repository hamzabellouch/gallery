package com.tkno.gallery.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Crop
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import kotlin.coroutines.resume

@OptIn(UnstableApi::class)
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
            } catch (_: Exception) {}
        }
        bitmaps
    }

    suspend fun trimVideo(
        context: Context,
        sourceUri: Uri,
        originalName: String,
        startMs: Long,
        endMs: Long,
        rotationAngle: Float = 0f,
        cropRectNormalized: RectF? = null
    ): Uri? = withContext(Dispatchers.IO) {
        val normalizedAngle = ((rotationAngle % 360f) + 360f) % 360f

        // If cropped or rotated, use Media3 Transformer with GPU effects
        if (cropRectNormalized != null || normalizedAngle != 0f) {
            val transformedUri = transformVideoWithMedia3(
                context = context,
                sourceUri = sourceUri,
                originalName = originalName,
                startMs = startMs,
                endMs = endMs,
                rotationAngle = normalizedAngle,
                cropRect = cropRectNormalized
            )
            if (transformedUri != null) return@withContext transformedUri
        }

        // Fast path for trim only (stream copy without re-encoding)
        return@withContext trimVideoWithMuxer(
            context = context,
            sourceUri = sourceUri,
            originalName = originalName,
            startMs = startMs,
            endMs = endMs,
            rotationAngle = normalizedAngle
        )
    }

    private suspend fun transformVideoWithMedia3(
        context: Context,
        sourceUri: Uri,
        originalName: String,
        startMs: Long,
        endMs: Long,
        rotationAngle: Float,
        cropRect: RectF?
    ): Uri? {
        val tempOutputFile = File(context.cacheDir, "transformed_${System.currentTimeMillis()}.mp4")
        try {
            val clippingBuilder = MediaItem.ClippingConfiguration.Builder()
            if (startMs > 0L) {
                clippingBuilder.setStartPositionMs(startMs)
            }
            if (endMs > startMs) {
                clippingBuilder.setEndPositionMs(endMs)
            }

            val mediaItem = MediaItem.Builder()
                .setUri(sourceUri)
                .setClippingConfiguration(clippingBuilder.build())
                .build()

            val effectsList = mutableListOf<Effect>()

            if (rotationAngle != 0f) {
                effectsList.add(
                    ScaleAndRotateTransformation.Builder()
                        .setRotationDegrees(rotationAngle)
                        .build()
                )
            }

            if (cropRect != null) {
                // Media3 Crop coordinates: left [-1..1], right [-1..1], bottom [-1..1], top [-1..1]
                val ndcLeft = (cropRect.left * 2f - 1f).coerceIn(-1f, 1f)
                val ndcRight = (cropRect.right * 2f - 1f).coerceIn(-1f, 1f)
                val ndcTop = (1f - cropRect.top * 2f).coerceIn(-1f, 1f)
                val ndcBottom = (1f - cropRect.bottom * 2f).coerceIn(-1f, 1f)
                effectsList.add(Crop(ndcLeft, ndcRight, ndcBottom, ndcTop))
            }

            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setEffects(Effects(emptyList(), effectsList))
                .build()

            val success = withContext(Dispatchers.Main) {
                suspendCancellableCoroutine<Boolean> { cont ->
                    val transformer = Transformer.Builder(context)
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                                if (cont.isActive) cont.resume(true)
                            }

                            override fun onError(
                                composition: Composition,
                                exportResult: ExportResult,
                                exportException: ExportException
                            ) {
                                exportException.printStackTrace()
                                if (cont.isActive) cont.resume(false)
                            }
                        })
                        .build()

                    cont.invokeOnCancellation {
                        try {
                            transformer.cancel()
                        } catch (_: Exception) {}
                    }

                    transformer.start(editedMediaItem, tempOutputFile.absolutePath)
                }
            }

            if (success && tempOutputFile.exists() && tempOutputFile.length() > 0) {
                return saveFileToMediaStore(context, tempOutputFile, originalName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            tempOutputFile.delete()
        }
        return null
    }

    private fun trimVideoWithMuxer(
        context: Context,
        sourceUri: Uri,
        originalName: String,
        startMs: Long,
        endMs: Long,
        rotationAngle: Float
    ): Uri? {
        var tempOutputFile: File? = null
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            val pfd = context.contentResolver.openFileDescriptor(sourceUri, "r") ?: return null
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
                return null
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
                try { retriever.release() } catch (_: Exception) {}
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

            return saveFileToMediaStore(context, tempOutputFile, originalName)
        } catch (e: Exception) {
            e.printStackTrace()
            try { muxer?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
            tempOutputFile?.delete()
            return null
        }
    }

    private fun saveFileToMediaStore(context: Context, sourceFile: File, originalName: String): Uri? {
        val baseName = originalName.substringBeforeLast(".")
        val newFileName = "${baseName}_edited_${System.currentTimeMillis()}.mp4"

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, newFileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Gallery")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val resultUri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (resultUri != null && sourceFile.exists()) {
            context.contentResolver.openOutputStream(resultUri)?.use { os ->
                FileInputStream(sourceFile).use { fis ->
                    fis.copyTo(os)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                context.contentResolver.update(resultUri, contentValues, null, null)
            }
        }

        sourceFile.delete()
        return resultUri
    }
}
