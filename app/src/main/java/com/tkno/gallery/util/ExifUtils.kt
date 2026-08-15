package com.tkno.gallery.util

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream

data class ExifData(
    val cameraMake: String = "",
    val cameraModel: String = "",
    val aperture: String = "",
    val iso: String = "",
    val exposureTime: String = "",
    val focalLength: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val megapixel: String = ""
)

object ExifUtils {

    fun readExif(context: Context, uri: Uri): ExifData {
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
                val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)

                val mp = if (width > 0 && height > 0) {
                    val megapixels = (width.toLong() * height.toLong()) / 1_000_000.0
                    String.format("%.1f MP", megapixels)
                } else ""

                ExifData(
                    cameraMake = make,
                    cameraModel = model,
                    aperture = if (aperture.isNotEmpty()) "f/$aperture" else "",
                    iso = if (iso.isNotEmpty()) "ISO $iso" else "",
                    exposureTime = if (expTime.isNotEmpty()) "${expTime}s" else "",
                    focalLength = if (focal.isNotEmpty()) "${focal}mm" else "",
                    width = width,
                    height = height,
                    megapixel = mp
                )
            } ?: ExifData()
        } catch (e: Exception) {
            e.printStackTrace()
            ExifData()
        }
    }
}
