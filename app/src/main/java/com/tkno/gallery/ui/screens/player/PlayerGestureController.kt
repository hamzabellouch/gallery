package com.tkno.gallery.ui.screens.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PlayerGestureController(
    modifier: Modifier = Modifier,
    onSingleTap: () -> Unit,
    onDoubleTapLeft: () -> Unit,
    onDoubleTapRight: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onGestureStart: () -> Unit = {},
    onGestureEnd: () -> Unit = {},
    onSwipeUp: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val activity = remember { context as? Activity }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = { offset ->
                        if (offset.x < size.width / 2) {
                            onDoubleTapLeft()
                        } else {
                            onDoubleTapRight()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    val screenWidth = size.width
                    val screenHeight = size.height

                    val isCenterZone = startX >= screenWidth * 0.25f && startX <= screenWidth * 0.75f
                    val isLeftSide = startX < screenWidth * 0.25f

                    // Retrieve initial state on touch down for high-precision accumulation
                    var initialBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
                    if (initialBrightness < 0f) {
                        initialBrightness = try {
                            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
                        } catch (_: Exception) {
                            0.5f
                        }
                    }
                    var brightnessAccumulator = initialBrightness.coerceIn(0.01f, 1.0f)

                    var volumeAccumulator = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                    var lastDispatchedVolInt = volumeAccumulator.roundToInt()

                    var totalDeltaY = 0f
                    var isVerticalDrag = false
                    var isHorizontalDrag = false
                    var hasTriggeredSwipeUp = false
                    var hasStartedGesture = false

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break

                            val deltaX = change.position.x - change.previousPosition.x
                            val deltaY = change.position.y - change.previousPosition.y

                            val absX = abs(deltaX)
                            val absY = abs(deltaY)

                            if (!isVerticalDrag && !isHorizontalDrag) {
                                if (absY > 8f && absY > absX * 1.3f) {
                                    isVerticalDrag = true
                                    if (!isCenterZone) {
                                        hasStartedGesture = true
                                        onGestureStart()
                                    }
                                } else if (absX > 8f && absX > absY * 1.3f) {
                                    isHorizontalDrag = true
                                }
                            }

                            if (isVerticalDrag) {
                                change.consume()
                                totalDeltaY += deltaY

                                if (isCenterZone) {
                                    if (!hasTriggeredSwipeUp && totalDeltaY < -50f) {
                                        hasTriggeredSwipeUp = true
                                        onSwipeUp()
                                    }
                                } else if (isLeftSide) {
                                    // Smooth brightness accumulation
                                    val dragScale = (screenHeight * 0.60f).coerceAtLeast(350f)
                                    val dragVal = -deltaY / dragScale
                                    brightnessAccumulator = (brightnessAccumulator + dragVal).coerceIn(0.01f, 1.0f)
                                    activity?.window?.attributes?.let { layoutParams ->
                                        layoutParams.screenBrightness = brightnessAccumulator
                                        activity.window.attributes = layoutParams
                                    }
                                    onBrightnessChange(brightnessAccumulator)
                                } else {
                                    // Smooth volume accumulation with continuous fraction feedback
                                    val dragScale = (screenHeight * 0.60f).coerceAtLeast(350f)
                                    val dragVal = -deltaY / dragScale
                                    volumeAccumulator = (volumeAccumulator + dragVal * maxVolume).coerceIn(0f, maxVolume.toFloat())
                                    val newVolInt = volumeAccumulator.roundToInt()
                                    if (newVolInt != lastDispatchedVolInt) {
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolInt, 0)
                                        lastDispatchedVolInt = newVolInt
                                    }
                                    onVolumeChange(volumeAccumulator / maxVolume.toFloat())
                                }
                            }
                            // For horizontal drag, do NOT consume change so HorizontalPager handles swipe!
                        }
                    } finally {
                        if (hasStartedGesture) {
                            onGestureEnd()
                        }
                    }
                }
            }
    ) {
        content()
    }
}
