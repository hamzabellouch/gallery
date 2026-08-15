package com.tkno.gallery.ui.screens.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
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

@Composable
fun PlayerGestureController(
    modifier: Modifier = Modifier,
    onSingleTap: () -> Unit,
    onDoubleTapLeft: () -> Unit,
    onDoubleTapRight: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onSwipeUp: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val activity = remember { context as? Activity }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

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

                    val isCenterZone = startX >= screenWidth * 0.25f && startX <= screenWidth * 0.75f
                    val isLeftSide = startX < screenWidth * 0.25f

                    var totalDeltaY = 0f
                    var isVerticalDrag = false
                    var isHorizontalDrag = false
                    var hasTriggeredSwipeUp = false

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
                                val dragVal = -deltaY / 400f
                                activity?.window?.attributes?.let { layoutParams ->
                                    var currentBrightness = layoutParams.screenBrightness
                                    if (currentBrightness < 0) currentBrightness = 0.5f
                                    val newBrightness = (currentBrightness + dragVal).coerceIn(0.01f, 1.0f)
                                    layoutParams.screenBrightness = newBrightness
                                    activity.window.attributes = layoutParams
                                    onBrightnessChange(newBrightness)
                                }
                            } else {
                                val dragVal = -deltaY / 400f
                                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                val deltaVol = (dragVal * maxVolume).toInt()
                                val newVol = (currentVol + deltaVol).coerceIn(0, maxVolume)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                onVolumeChange(newVol.toFloat() / maxVolume)
                            }
                        }
                        // For horizontal drag, do NOT consume change so HorizontalPager handles swipe!
                    }
                }
            }
    ) {
        content()
    }
}
