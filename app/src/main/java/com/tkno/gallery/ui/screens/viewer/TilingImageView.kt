package com.tkno.gallery.ui.screens.viewer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

@Composable
fun TilingImageView(
    imageUri: Uri,
    modifier: Modifier = Modifier,
    resetZoomTrigger: Int = 0,
    onTap: () -> Unit = {},
    onSwipeUp: () -> Unit = {},
    onScaleChanged: (Float) -> Unit = {}
) {
    val context = LocalContext.current

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(resetZoomTrigger) {
        if (resetZoomTrigger > 0) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    LaunchedEffect(scale) {
        onScaleChanged(scale)
    }

    var fullBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    // Multi-Decoder Fallback Pipeline: Handles JPEG, PNG, WEBP, HEIC, HEIF, AVIF, DNG, RAW, BMP
    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            try {
                var loadedBitmap: Bitmap? = null

                // 1. Try ImageDecoder on Android P+ for HEIF, AVIF, WEBP, DNG, RAW, JPEG, PNG
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        val source = ImageDecoder.createSource(context.contentResolver, imageUri)
                        loadedBitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                            imageSize = IntSize(info.size.width, info.size.height)
                            decoder.isMutableRequired = false
                        }
                    } catch (e: Exception) {
                        loadedBitmap = null
                    }
                }

                // 2. Fallback to BitmapFactory with memory bounds checking
                if (loadedBitmap == null) {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
                    inputStream?.use { stream ->
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeStream(stream, null, options)
                        imageSize = IntSize(options.outWidth, options.outHeight)
                    }

                    val stream2: InputStream? = context.contentResolver.openInputStream(imageUri)
                    stream2?.use { stream ->
                        val decodeOptions = BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                        loadedBitmap = BitmapFactory.decodeStream(stream, null, decodeOptions)
                    }
                }

                fullBitmap = loadedBitmap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val currentBitmap = fullBitmap

    if (currentBitmap != null) {
        Canvas(
            modifier = modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onDoubleTap = {
                            scale = if (scale > 1.5f) 1f else 3.5f
                            if (scale == 1f) offset = Offset.Zero
                        }
                    )
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalDeltaY = 0f
                        var isVerticalDrag = false
                        var isHorizontalDrag = false
                        var hasTriggeredSwipeUp = false

                        do {
                            val event = awaitPointerEvent()
                            val canceled = event.changes.any { it.isConsumed }
                            if (canceled) break

                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val pointerCount = event.changes.size

                            if (pointerCount > 1 || scale > 1.05f) {
                                if (zoomChange != 1f || panChange != Offset.Zero) {
                                    event.changes.forEach { it.consume() }

                                    val newScale = (scale * zoomChange).coerceIn(1f, 10f)
                                    scale = newScale
                                    if (newScale > 1f) {
                                        offset += panChange
                                    } else {
                                        offset = Offset.Zero
                                    }
                                }
                            } else {
                                val deltaX = panChange.x
                                val deltaY = panChange.y
                                val absX = kotlin.math.abs(deltaX)
                                val absY = kotlin.math.abs(deltaY)

                                if (!isVerticalDrag && !isHorizontalDrag) {
                                    if (absY > 8f && absY > absX * 1.3f) {
                                        isVerticalDrag = true
                                    } else if (absX > 8f && absX > absY * 1.3f) {
                                        isHorizontalDrag = true
                                    }
                                }

                                if (isVerticalDrag) {
                                    totalDeltaY += deltaY
                                    if (!hasTriggeredSwipeUp && totalDeltaY < -50f) {
                                        hasTriggeredSwipeUp = true
                                        onSwipeUp()
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val bitmapWidth = currentBitmap.width.toFloat()
            val bitmapHeight = currentBitmap.height.toFloat()

            val baseScale = minOf(canvasWidth / bitmapWidth, canvasHeight / bitmapHeight)
            val dx = (canvasWidth - bitmapWidth * baseScale) / 2f
            val dy = (canvasHeight - bitmapHeight * baseScale) / 2f

            translate(left = offset.x, top = offset.y) {
                scale(scale = scale, pivot = center) {
                    translate(left = dx, top = dy) {
                        scale(scale = baseScale, pivot = Offset.Zero) {
                            drawImage(image = currentBitmap.asImageBitmap())
                        }
                    }
                }
            }
        }
    }
}
