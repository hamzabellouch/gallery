package com.tkno.gallery.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tkno.gallery.ui.screens.home.Tab
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ClassicBottomBar(
    tabs: SnapshotStateList<Tab>,
    currentTab: Tab,
    onTabSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier,
    onOrderChanged: (List<Tab>) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var currentDragOffset by remember { mutableFloatStateOf(0f) }
    var itemWidthPx by remember { mutableFloatStateOf(0f) }

    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
    val indicatorCapsuleColor = MaterialTheme.colorScheme.secondaryContainer
    val navContainerColor = MaterialTheme.colorScheme.background

    NavigationBar(
        containerColor = navContainerColor,
        tonalElevation = 0.dp,
        modifier = modifier
            .height(72.dp)
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val totalWidth = coordinates.size.width.toFloat()
                if (totalWidth > 0) {
                    itemWidthPx = totalWidth / 4f
                }
            }
    ) {
        tabs.forEachIndexed { index, tab ->
            val info = getTabInfo(tab)
            val isSelected = currentTab == tab
            val isDragging = draggingIndex == index

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelect(tab) },
                icon = {
                    Icon(
                        imageVector = info.icon,
                        contentDescription = info.label,
                        tint = if (isSelected) activeColor else inactiveColor
                    )
                },
                label = {
                    Text(
                        text = info.label,
                        color = if (isSelected) activeColor else inactiveColor,
                        fontSize = 12.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = indicatorCapsuleColor
                ),
                modifier = Modifier
                    .graphicsLayer {
                        if (isDragging) {
                            translationX = currentDragOffset
                            scaleX = 1.12f
                            scaleY = 1.12f
                        }
                    }
                    .pointerInput(index) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var isLongPressActive = false

                            val longPressTimer = scope.launch {
                                delay(viewConfiguration.longPressTimeoutMillis)
                                isLongPressActive = true
                                draggingIndex = index
                                currentDragOffset = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }

                            val pointer = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointer }

                                if (change == null || !change.pressed) {
                                    longPressTimer.cancel()
                                    if (isLongPressActive) {
                                        draggingIndex = null
                                        currentDragOffset = 0f
                                    }
                                    break
                                }

                                if (!isLongPressActive) {
                                    val diff = change.position - down.position
                                    if (diff.getDistance() > viewConfiguration.touchSlop) {
                                        longPressTimer.cancel()
                                    }
                                } else {
                                    change.consume()
                                    val deltaX = change.position.x - change.previousPosition.x
                                    val activeIndex = draggingIndex ?: break
                                    currentDragOffset += deltaX

                                    val threshold = if (itemWidthPx > 0f) itemWidthPx * 0.5f else 100f

                                    if (currentDragOffset > threshold && activeIndex < tabs.size - 1) {
                                        val temp = tabs[activeIndex]
                                        tabs[activeIndex] = tabs[activeIndex + 1]
                                        tabs[activeIndex + 1] = temp
                                        draggingIndex = activeIndex + 1
                                        currentDragOffset -= itemWidthPx
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onOrderChanged(tabs.toList())
                                    } else if (currentDragOffset < -threshold && activeIndex > 0) {
                                        val temp = tabs[activeIndex]
                                        tabs[activeIndex] = tabs[activeIndex - 1]
                                        tabs[activeIndex - 1] = temp
                                        draggingIndex = activeIndex - 1
                                        currentDragOffset += itemWidthPx
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onOrderChanged(tabs.toList())
                                    }
                                }
                            }
                        }
                    }
            )
        }
    }
}
