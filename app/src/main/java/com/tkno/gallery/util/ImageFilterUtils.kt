package com.tkno.gallery.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix as AndroidColorMatrix
import android.graphics.ColorMatrixColorFilter as AndroidColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

enum class FilterType(val displayName: String) {
    NONE("None"),
    VIVID("Vivid"),
    PLAYA("Playa"),
    HONEY("Honey"),
    ISLA("Isla"),
    DESERT("Desert"),
    MONO("Mono"),
    METRO("Metro");

    fun getComposeColorMatrix(): ComposeColorMatrix? {
        val array = getMatrixArray() ?: return null
        return ComposeColorMatrix(array)
    }

    fun getAndroidColorMatrix(): AndroidColorMatrix? {
        val array = getMatrixArray() ?: return null
        return AndroidColorMatrix(array)
    }

    private fun getMatrixArray(): FloatArray? {
        return when (this) {
            NONE -> null
            VIVID -> floatArrayOf(
                1.25f, 0f, 0f, 0f, 0f,
                0f, 1.25f, 0f, 0f, 0f,
                0f, 0f, 1.25f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            PLAYA -> floatArrayOf(
                1.2f, 0f, 0f, 0f, 10f,
                0f, 1.08f, 0f, 0f, 5f,
                0f, 0f, 0.88f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            )
            HONEY -> floatArrayOf(
                1.3f, 0f, 0f, 0f, 15f,
                0f, 1.15f, 0f, 0f, 10f,
                0f, 0f, 0.72f, 0f, -20f,
                0f, 0f, 0f, 1f, 0f
            )
            ISLA -> floatArrayOf(
                0.88f, 0f, 0f, 0f, -5f,
                0f, 1.12f, 0f, 0f, 10f,
                0f, 0f, 1.28f, 0f, 20f,
                0f, 0f, 0f, 1f, 0f
            )
            DESERT -> floatArrayOf(
                1.22f, 0f, 0f, 0f, 12f,
                0f, 1.05f, 0f, 0f, 4f,
                0f, 0f, 0.82f, 0f, -15f,
                0f, 0f, 0f, 1f, 0f
            )
            MONO -> floatArrayOf(
                0.33f, 0.59f, 0.11f, 0f, 0f,
                0.33f, 0.59f, 0.11f, 0f, 0f,
                0.33f, 0.59f, 0.11f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            METRO -> floatArrayOf(
                1.15f, 0f, 0f, 0f, -10f,
                0f, 1.15f, 0f, 0f, -10f,
                0f, 0f, 1.35f, 0f, 5f,
                0f, 0f, 0f, 1f, 0f
            )
        }
    }
}

object ImageFilterUtils {

    suspend fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int = 1200,
        reqHeight: Int = 1200
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            } ?: return@withContext null

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            var bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            } ?: return@withContext null

            // Check EXIF orientation
            val orientation = getExifOrientation(context, uri)
            if (orientation != 0) {
                val matrix = Matrix().apply { postRotate(orientation.toFloat()) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated != bitmap) {
                    bitmap.recycle()
                    bitmap = rotated
                }
            }

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun getExifOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val rotationDegrees = exif.rotationDegrees
                if (rotationDegrees != 0) {
                    rotationDegrees
                } else {
                    when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    suspend fun saveEditedImage(
        context: Context,
        originalUri: Uri,
        originalName: String,
        rotationAngle: Float,
        cropRectNormalized: RectF?,
        filterType: FilterType
    ): Uri? = withContext(Dispatchers.IO) {
        var sourceBitmap: Bitmap? = null
        var finalBitmap: Bitmap? = null
        try {
            // 1. Decode source bitmap with full resolution
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            sourceBitmap = context.contentResolver.openInputStream(originalUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return@withContext null

            // 2. Compute combined orientation and user rotation
            val exifOrientation = getExifOrientation(context, originalUri)
            val totalRotation = (((exifOrientation + rotationAngle) % 360f) + 360f) % 360f

            val rawWidth = sourceBitmap.width
            val rawHeight = sourceBitmap.height

            // Calculate bounding box after total rotation
            val rotBounds = RectF(0f, 0f, rawWidth.toFloat(), rawHeight.toFloat())
            if (totalRotation != 0f) {
                val rotMatrix = Matrix().apply { postRotate(totalRotation) }
                rotMatrix.mapRect(rotBounds)
            }

            val totalRotatedWidth = rotBounds.width().roundToInt().coerceAtLeast(1)
            val totalRotatedHeight = rotBounds.height().roundToInt().coerceAtLeast(1)

            // Determine crop boundaries in rotated coordinates
            val isCropValid = cropRectNormalized != null &&
                    (cropRectNormalized.left > 0.0001f || cropRectNormalized.top > 0.0001f ||
                     cropRectNormalized.right < 0.9999f || cropRectNormalized.bottom < 0.9999f)

            val cropLeft: Int
            val cropTop: Int
            val cropWidth: Int
            val cropHeight: Int

            if (isCropValid) {
                val crop = cropRectNormalized!!
                val l = (crop.left.coerceIn(0f, 1f) * totalRotatedWidth).roundToInt()
                val t = (cropRectNormalized.top.coerceIn(0f, 1f) * totalRotatedHeight).roundToInt()
                val r = (cropRectNormalized.right.coerceIn(0f, 1f) * totalRotatedWidth).roundToInt()
                val b = (cropRectNormalized.bottom.coerceIn(0f, 1f) * totalRotatedHeight).roundToInt()

                cropLeft = l.coerceIn(0, totalRotatedWidth - 1)
                cropTop = t.coerceIn(0, totalRotatedHeight - 1)
                cropWidth = (r - cropLeft).coerceIn(1, totalRotatedWidth - cropLeft)
                cropHeight = (b - cropTop).coerceIn(1, totalRotatedHeight - cropTop)
            } else {
                cropLeft = 0
                cropTop = 0
                cropWidth = totalRotatedWidth
                cropHeight = totalRotatedHeight
            }

            val androidColorMatrix = filterType.getAndroidColorMatrix()
            val needsTransform = totalRotation != 0f || isCropValid || androidColorMatrix != null

            if (!needsTransform) {
                // Direct pass-through without any intermediate allocations
                finalBitmap = sourceBitmap
                sourceBitmap = null
            } else {
                // 3. Single-pass unified transformation pipeline: Rotation + Translation + Crop + ColorMatrix
                val destBitmap = Bitmap.createBitmap(cropWidth, cropHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(destBitmap)

                val transformMatrix = Matrix().apply {
                    if (totalRotation != 0f) {
                        postRotate(totalRotation)
                    }
                    postTranslate(-rotBounds.left - cropLeft, -rotBounds.top - cropTop)
                }

                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
                if (androidColorMatrix != null) {
                    paint.colorFilter = AndroidColorMatrixColorFilter(androidColorMatrix)
                }

                canvas.drawBitmap(sourceBitmap, transformMatrix, paint)

                // Immediately recycle sourceBitmap to free memory before compression
                sourceBitmap.recycle()
                sourceBitmap = null
                finalBitmap = destBitmap
            }

            // 4. Save to MediaStore
            val baseName = originalName.substringBeforeLast(".")
            val newFileName = "${baseName}_edited_${System.currentTimeMillis()}.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, newFileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Gallery")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resultUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (resultUri != null) {
                context.contentResolver.openOutputStream(resultUri)?.use { outputStream ->
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(resultUri, contentValues, null, null)
                }
            }

            resultUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                sourceBitmap?.recycle()
            } catch (_: Exception) {}
            try {
                finalBitmap?.recycle()
            } catch (_: Exception) {}
        }
    }
}
