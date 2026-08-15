package com.tkno.gallery.util

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.tkno.gallery.data.model.MediaItem
import com.tkno.gallery.data.model.ResolutionBadge
import java.io.File
import java.io.InputStream
import java.util.Locale

data class ComprehensiveMetadata(
    val filename: String = "",
    val fullDirectory: String = "",
    val parentFolder: String = "",
    val resolution: String = "",
    val badgeLabel: String = "",
    val fps: String = "",
    val aspectRatio: String = "",
    val fileSize: String = "",
    val dateAdded: String = "",
    val mimeType: String = "",

    // Video Specific
    val isVideo: Boolean = false,
    val duration: String = "",
    val bitrate: String = "",
    val videoCodec: String = "",
    val audioCodec: String = "",
    val rotation: String = "",

    // Image / EXIF Specific
    val megapixels: String = "",
    val cameraMake: String = "",
    val cameraModel: String = "",
    val aperture: String = "",
    val iso: String = "",
    val exposureTime: String = "",
    val focalLength: String = "",
    val focalLength35mm: String = "",
    val flash: String = "",
    val whiteBalance: String = "",
    val colorSpace: String = "",
    val gpsCoordinates: String = ""
)

object MediaMetadataUtils {

    fun extractMetadata(context: Context, item: MediaItem): ComprehensiveMetadata {
        val badge = item.resolutionBadge
        val badgeStr = if (badge != ResolutionBadge.NONE) badge.label else ""

        // Full Directory & Parent Folder
        val (directoryPath, parentFolderName) = extractDirectoryAndFolder(item)

        // Aspect Ratio
        val aspectRatioStr = calculateAspectRatio(item.width, item.height)

        if (item.isVideo) {
            val videoMeta = extractVideoDetails(context, item.uri)
            val resStr = if (item.width > 0 && item.height > 0) "${item.width} x ${item.height}" else videoMeta.resolution
            val fpsStr = videoMeta.fps

            return ComprehensiveMetadata(
                filename = item.name,
                fullDirectory = directoryPath,
                parentFolder = parentFolderName.ifEmpty { item.albumName },
                resolution = resStr,
                badgeLabel = badgeStr,
                fps = fpsStr,
                aspectRatio = aspectRatioStr,
                fileSize = FormatUtils.formatFileSize(item.size),
                dateAdded = FormatUtils.formatDateShort(item.dateAddedSec),
                mimeType = item.mimeType.ifEmpty { "video/*" },
                isVideo = true,
                duration = if (item.durationMs > 0) FormatUtils.formatDuration(item.durationMs) else videoMeta.duration,
                bitrate = videoMeta.bitrate,
                videoCodec = videoMeta.videoCodec,
                audioCodec = videoMeta.audioCodec,
                rotation = videoMeta.rotation
            )
        } else {
            val exif = extractImageExif(context, item.uri, item.width, item.height)
            val resStr = if (item.width > 0 && item.height > 0) "${item.width} x ${item.height}" else exif.resolution

            return ComprehensiveMetadata(
                filename = item.name,
                fullDirectory = directoryPath,
                parentFolder = parentFolderName.ifEmpty { item.albumName },
                resolution = resStr,
                badgeLabel = badgeStr,
                fps = "",
                aspectRatio = aspectRatioStr,
                fileSize = FormatUtils.formatFileSize(item.size),
                dateAdded = FormatUtils.formatDateShort(item.dateAddedSec),
                mimeType = item.mimeType.ifEmpty { "image/*" },
                isVideo = false,
                megapixels = exif.megapixels,
                cameraMake = exif.cameraMake,
                cameraModel = exif.cameraModel,
                aperture = exif.aperture,
                iso = exif.iso,
                exposureTime = exif.exposureTime,
                focalLength = exif.focalLength,
                focalLength35mm = exif.focalLength35mm,
                flash = exif.flash,
                whiteBalance = exif.whiteBalance,
                colorSpace = exif.colorSpace,
                gpsCoordinates = exif.gpsCoordinates
            )
        }
    }

    private fun extractDirectoryAndFolder(item: MediaItem): Pair<String, String> {
        val rawPath = item.path
        if (rawPath.isNotEmpty()) {
            val file = File(rawPath)
            val parentFile = file.parentFile
            if (parentFile != null) {
                return Pair(parentFile.absolutePath + "/", parentFile.name)
            }
        }

        // Uri path fallback
        val uriPath = item.uri.path ?: ""
        val lastSlash = uriPath.lastIndexOf('/')
        if (lastSlash > 0) {
            val dir = uriPath.substring(0, lastSlash)
            val folderName = dir.substringAfterLast('/')
            return Pair(dir + "/", folderName)
        }

        return Pair("/Internal Storage/", item.albumName)
    }

    private fun calculateAspectRatio(w: Int, h: Int): String {
        if (w <= 0 || h <= 0) return ""
        fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
        val divisor = gcd(w, h)
        val rw = w / divisor
        val rh = h / divisor

        val ratioFloat = w.toFloat() / h.toFloat()
        return when {
            kotlin.math.abs(ratioFloat - 1.777f) < 0.05f || kotlin.math.abs(ratioFloat - 0.562f) < 0.05f -> "16:9"
            kotlin.math.abs(ratioFloat - 1.333f) < 0.05f || kotlin.math.abs(ratioFloat - 0.750f) < 0.05f -> "4:3"
            kotlin.math.abs(ratioFloat - 1.0f) < 0.02f -> "1:1"
            kotlin.math.abs(ratioFloat - 2.333f) < 0.08f -> "21:9"
            else -> "$rw:$rh"
        }
    }

    private data class InternalVideoMeta(
        val resolution: String = "",
        val fps: String = "",
        val duration: String = "",
        val bitrate: String = "",
        val videoCodec: String = "",
        val audioCodec: String = "",
        val rotation: String = ""
    )

    private fun extractVideoDetails(context: Context, uri: Uri): InternalVideoMeta {
        var retriever: MediaMetadataRetriever? = null
        var fpsVal = ""
        var resolutionVal = ""
        var durationVal = ""
        var bitrateVal = ""
        var mimeVal = ""
        var rotationVal = ""
        var audioCodecVal = ""

        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)

            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            if (w != null && h != null) {
                resolutionVal = "$w x $h"
            }

            val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            if (!rot.isNullOrEmpty()) {
                rotationVal = "$rot°"
            }

            val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (!dur.isNullOrEmpty()) {
                val durMs = dur.toLongOrNull() ?: 0L
                durationVal = FormatUtils.formatDuration(durMs)
            }

            val bit = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            if (!bit.isNullOrEmpty()) {
                val bps = bit.toLongOrNull() ?: 0L
                bitrateVal = if (bps > 1_000_000) {
                    String.format(Locale.US, "%.1f Mbps", bps / 1_000_000.0)
                } else {
                    "${bps / 1000} Kbps"
                }
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val fpsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                if (!fpsStr.isNullOrEmpty()) {
                    val fpsF = fpsStr.toFloatOrNull()
                    if (fpsF != null && fpsF > 0) {
                        fpsVal = "${fpsF.toInt()} FPS"
                    }
                }
            }

            mimeVal = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""
        } catch (_: Exception) {
        } finally {
            try { retriever?.release() } catch (_: Exception) {}
        }

        // FPS & Codec via MediaExtractor if not retrieved
        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    if (fpsVal.isEmpty() && format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        val fpsInt = format.getInteger(MediaFormat.KEY_FRAME_RATE)
                        fpsVal = "$fpsInt FPS"
                    }
                    if (mimeVal.isEmpty() || mimeVal.startsWith("video/")) {
                        mimeVal = formatMimeCodec(mime)
                    }
                } else if (mime.startsWith("audio/")) {
                    audioCodecVal = formatMimeCodec(mime)
                }
            }
            extractor.release()
        } catch (_: Exception) {}

        return InternalVideoMeta(
            resolution = resolutionVal,
            fps = fpsVal,
            duration = durationVal,
            bitrate = bitrateVal,
            videoCodec = mimeVal,
            audioCodec = audioCodecVal,
            rotation = rotationVal
        )
    }

    private fun formatMimeCodec(mime: String): String {
        return when {
            mime.contains("avc", ignoreCase = true) || mime.contains("h264", ignoreCase = true) -> "H.264 / AVC"
            mime.contains("hevc", ignoreCase = true) || mime.contains("h265", ignoreCase = true) -> "H.265 / HEVC"
            mime.contains("vp9", ignoreCase = true) -> "VP9"
            mime.contains("vp8", ignoreCase = true) -> "VP8"
            mime.contains("av01", ignoreCase = true) || mime.contains("av1", ignoreCase = true) -> "AV1"
            mime.contains("mp4v", ignoreCase = true) -> "MPEG-4"
            mime.contains("aac", ignoreCase = true) -> "AAC"
            mime.contains("mp3", ignoreCase = true) || mime.contains("mpeg", ignoreCase = true) -> "MP3"
            mime.contains("opus", ignoreCase = true) -> "Opus"
            mime.contains("flac", ignoreCase = true) -> "FLAC"
            else -> mime.substringAfter('/')
        }
    }

    private data class InternalImageExif(
        val resolution: String = "",
        val megapixels: String = "",
        val cameraMake: String = "",
        val cameraModel: String = "",
        val aperture: String = "",
        val iso: String = "",
        val exposureTime: String = "",
        val focalLength: String = "",
        val focalLength35mm: String = "",
        val flash: String = "",
        val whiteBalance: String = "",
        val colorSpace: String = "",
        val gpsCoordinates: String = ""
    )

    private fun extractImageExif(context: Context, uri: Uri, fallbackW: Int, fallbackH: Int): InternalImageExif {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val exif = ExifInterface(stream)
                val make = exif.getAttribute(ExifInterface.TAG_MAKE) ?: ""
                val model = exif.getAttribute(ExifInterface.TAG_MODEL) ?: ""
                val aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER) ?: ""
                val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY) ?: ""
                val expTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME) ?: ""
                val focal = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) ?: ""
                val focal35 = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM) ?: ""
                val flashAttr = exif.getAttributeInt(ExifInterface.TAG_FLASH, -1)
                val wbAttr = exif.getAttributeInt(ExifInterface.TAG_WHITE_BALANCE, -1)
                val csAttr = exif.getAttributeInt(ExifInterface.TAG_COLOR_SPACE, -1)

                var w = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                var h = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
                if (w <= 0) w = fallbackW
                if (h <= 0) h = fallbackH

                val mp = if (w > 0 && h > 0) {
                    val megapixels = (w.toLong() * h.toLong()) / 1_000_000.0
                    String.format(Locale.US, "%.1f MP", megapixels)
                } else ""

                val flashStr = when (flashAttr) {
                    0 -> "Off, Did not fire"
                    1 -> "Fired"
                    else -> if (flashAttr > 0) "Fired" else ""
                }

                val wbStr = when (wbAttr) {
                    ExifInterface.WHITE_BALANCE_AUTO.toInt() -> "Auto"
                    ExifInterface.WHITE_BALANCE_MANUAL.toInt() -> "Manual"
                    else -> ""
                }

                val csStr = when (csAttr) {
                    ExifInterface.COLOR_SPACE_S_RGB -> "sRGB"
                    ExifInterface.COLOR_SPACE_UNCALIBRATED -> "Uncalibrated / P3"
                    else -> ""
                }

                // GPS
                val latLongArray = exif.latLong
                val gpsStr = if (latLongArray != null && latLongArray.size >= 2) {
                    String.format(Locale.US, "%.4f°, %.4f°", latLongArray[0], latLongArray[1])
                } else ""

                InternalImageExif(
                    resolution = if (w > 0 && h > 0) "$w x $h" else "",
                    megapixels = mp,
                    cameraMake = make,
                    cameraModel = model,
                    aperture = if (aperture.isNotEmpty()) "f/$aperture" else "",
                    iso = if (iso.isNotEmpty()) "ISO $iso" else "",
                    exposureTime = if (expTime.isNotEmpty()) {
                        val expFloat = expTime.toFloatOrNull()
                        if (expFloat != null && expFloat < 1.0f && expFloat > 0f) {
                            "1/${(1.0f / expFloat).toInt()}s"
                        } else "${expTime}s"
                    } else "",
                    focalLength = if (focal.isNotEmpty()) "${focal}mm" else "",
                    focalLength35mm = if (focal35.isNotEmpty()) "${focal35}mm eq." else "",
                    flash = flashStr,
                    whiteBalance = wbStr,
                    colorSpace = csStr,
                    gpsCoordinates = gpsStr
                )
            } ?: InternalImageExif()
        } catch (e: Exception) {
            e.printStackTrace()
            InternalImageExif()
        }
    }
}
