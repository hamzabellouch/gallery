package com.tkno.gallery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tkno.gallery.ui.screens.home.Tab
import com.tkno.gallery.ui.screens.home.TabInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun getTabInfo(tab: Tab): TabInfo {
    return when (tab) {
        Tab.Media -> TabInfo(Tab.Media, "Gallery", GalleryThumbnailIcon)
        Tab.Console -> TabInfo(Tab.Console, "Albums", AutoAwesomeMosaicIcon)
        Tab.Videos -> TabInfo(Tab.Videos, "Videos", VideoTemplateIcon)
        Tab.Images -> TabInfo(Tab.Images, "Images", ImageIcon)
    }
}

@Composable
fun FloatingBottomBar(
    tabs: SnapshotStateList<Tab>,
    currentTab: Tab,
    onTabSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier,
    onOrderChanged: (List<Tab>) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var currentDragOffset by remember { mutableFloatStateOf(0f) }
    var itemWidthPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(bottom = 16.dp, start = 20.dp, end = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Pill Navigation Bar Dock - تصميم عائم بشكل كبسولة
        Surface(
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
            shape = RoundedCornerShape(percent = 50),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .height(64.dp)
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    val totalWidth = coordinates.size.width.toFloat()
                    if (totalWidth > 0 && tabs.isNotEmpty()) {
                        itemWidthPx = (totalWidth - 12f) / tabs.size.toFloat()
                    }
                },
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp)
            ) {
                val itemCount = tabs.size
                if (itemCount > 0) {
                    val slotWidth = maxWidth / itemCount

                    val activeIndex = tabs.indexOf(currentTab).let { if (it == -1) 0 else it }
                    val clampedIndex = activeIndex.coerceIn(0, itemCount - 1)
                    val targetOffsetX = slotWidth * clampedIndex

                    // أنيميشن حركة الهالة الانزلاقية (Spring Physics)
                    val animatedOffsetX by animateDpAsState(
                        targetValue = targetOffsetX,
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "indicatorOffsetX"
                    )

                    // عنصر الهالة / المؤشر الدائري المنزلق في الخلفية
                    Box(
                        modifier = Modifier
                            .offset(x = animatedOffsetX)
                            .width(slotWidth)
                            .fillMaxHeight()
                            .padding(horizontal = 2.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val info = getTabInfo(tab)
                        val isSelected = currentTab == tab
                        val isDragging = draggingIndex == index

                        val iconTint by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(250),
                            label = "iconTint",
                        )

                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            animationSpec = tween(250),
                            label = "textColor",
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    if (isDragging) {
                                        translationX = currentDragOffset
                                        scaleX = 1.12f
                                        scaleY = 1.12f
                                        shadowElevation = 8f
                                    }
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    onTabSelect(tab)
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
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            ) {
                                Icon(
                                    imageVector = info.icon,
                                    contentDescription = info.label,
                                    tint = iconTint,
                                    modifier = Modifier.size(22.dp),
                                )
                                Text(
                                    text = info.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                    ),
                                    color = textColor,
                                    modifier = Modifier.padding(top = 2.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
