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
import java.io.InputStream

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
            var inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            inputStream = context.contentResolver.openInputStream(uri)
            var bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            if (bitmap != null) {
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

    private fun getExifOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
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
        try {
            // 1. Decode full quality bitmap
            val inputStream = context.contentResolver.openInputStream(originalUri) ?: return@withContext null
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            var sourceBitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            if (sourceBitmap == null) return@withContext null

            // Correct EXIF orientation first if present
            val exifOrientation = getExifOrientation(context, originalUri)
            if (exifOrientation != 0) {
                val matrix = Matrix().apply { postRotate(exifOrientation.toFloat()) }
                val rotated = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, matrix, true)
                if (rotated != sourceBitmap) {
                    sourceBitmap.recycle()
                    sourceBitmap = rotated
                }
            }

            // 2. Apply Rotation if specified (applied before crop so normalized crop coords match the oriented image)
            val normalizedRotation = ((rotationAngle % 360) + 360) % 360
            if (normalizedRotation != 0f) {
                val matrix = Matrix().apply { postRotate(normalizedRotation) }
                val rotated = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, matrix, true)
                if (rotated != sourceBitmap) {
                    sourceBitmap.recycle()
                    sourceBitmap = rotated
                }
            }

            // 3. Apply Crop if specified
            if (cropRectNormalized != null) {
                val left = (cropRectNormalized.left.coerceIn(0f, 1f) * sourceBitmap.width).toInt()
                val top = (cropRectNormalized.top.coerceIn(0f, 1f) * sourceBitmap.height).toInt()
                val right = (cropRectNormalized.right.coerceIn(0f, 1f) * sourceBitmap.width).toInt()
                val bottom = (cropRectNormalized.bottom.coerceIn(0f, 1f) * sourceBitmap.height).toInt()

                val cropWidth = (right - left).coerceAtLeast(1).coerceAtMost(sourceBitmap.width - left)
                val cropHeight = (bottom - top).coerceAtLeast(1).coerceAtMost(sourceBitmap.height - top)

                val cropped = Bitmap.createBitmap(sourceBitmap, left, top, cropWidth, cropHeight)
                if (cropped != sourceBitmap) {
                    sourceBitmap.recycle()
                    sourceBitmap = cropped
                }
            }

            // 4. Apply Filter if specified
            val androidColorMatrix = filterType.getAndroidColorMatrix()
            var finalBitmap = sourceBitmap
            if (androidColorMatrix != null) {
                val filtered = Bitmap.createBitmap(sourceBitmap.width, sourceBitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(filtered)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    colorFilter = AndroidColorMatrixColorFilter(androidColorMatrix)
                }
                canvas.drawBitmap(sourceBitmap, 0f, 0f, paint)
                if (sourceBitmap != filtered) {
                    sourceBitmap.recycle()
                }
                finalBitmap = filtered
            }

            // 5. Save to MediaStore
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

            finalBitmap.recycle()
            resultUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
