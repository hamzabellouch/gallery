package com.tkno.gallery.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tkno.gallery.data.model.MediaItem
import com.tkno.gallery.theme.TaskbarActivePrimaryDark
import com.tkno.gallery.theme.TaskbarActivePrimaryLight
import com.tkno.gallery.ui.screens.home.GalleryGridLevel
import com.tkno.gallery.util.FormatUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Ultra-Smooth Google Photos-Style Timeline FastScrollbar:
 *
 * 1. Exact Timeline Alignment: 100% accurate 1:1 mapping between on-screen rows/sections and floating date pill.
 * 2. Instant Zero-Lag Response: Smoothly tracks and jumps to target positions with 0ms UI delay.
 * 3. Interactive Floating Date Pill: Glides next to user's finger displaying Month & Year (e.g. "February 2025" / "فبراير ٢٠٢٥").
 * 4. Tactile Haptic Ticks: Triggers subtle haptic feedback whenever a date/month boundary is crossed.
 */
@Composable
fun FastScrollbar(
    gridState: LazyGridState,
    mediaItems: List<MediaItem>,
    modifier: Modifier = Modifier,
    groupedMedia: Map<String, List<MediaItem>>? = null,
    gridLevel: GalleryGridLevel = GalleryGridLevel.Large,
    columnCount: Int = 3,
    rowsPerCycle: Int = 50, // Kept for API compatibility
    accentColor: Color? = null
) {
    if (mediaItems.isEmpty() && (groupedMedia == null || groupedMedia.isEmpty())) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // Resolve matching active theme accent color
    val prefs = remember(context) { context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE) }
    val isDynamicColor = prefs.getBoolean("dynamic_color", true)
    val dynamicColorIcons = prefs.getBoolean("dynamic_color_icons", false)
    val applyDynamicToIcons = isDynamicColor && dynamicColorIcons
    val isDark = isSystemInDarkTheme()

    val primaryColor = accentColor ?: if (applyDynamicToIcons) {
        MaterialTheme.colorScheme.primary
    } else {
        if (isDark) TaskbarActivePrimaryDark else TaskbarActivePrimaryLight
    }

    var isDragging by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    var trackHeightPx by remember { mutableFloatStateOf(0f) }

    val handleSizeDp = 38.dp
    val handleSizePx = remember(density) { with(density) { handleSizeDp.toPx() } }

    var scrollJob by remember { mutableStateOf<Job?>(null) }
    var lastMonthText by remember { mutableStateOf("") }

    // Auto-hide timer: Stays visible while scrolling/dragging and smoothly fades out after 2.5s of rest
    LaunchedEffect(gridState.isScrollInProgress, isDragging) {
        if (gridState.isScrollInProgress || isDragging) {
            isVisible = true
        } else {
            delay(2500)
            isVisible = false
        }
    }

    // Build exact 1:1 array of timestamps matching every item in LazyVerticalGrid (headers + media items)
    val gridTimestamps = remember(groupedMedia, mediaItems) {
        if (groupedMedia != null && groupedMedia.isNotEmpty()) {
            val total = groupedMedia.size + groupedMedia.values.sumOf { it.size }
            val array = LongArray(total)
            var idx = 0
            for ((_, items) in groupedMedia) {
                val headerTs = items.firstOrNull()?.dateAddedSec ?: 0L
                array[idx++] = headerTs
                for (item in items) {
                    array[idx++] = item.dateAddedSec
                }
            }
            array
        } else {
            LongArray(mediaItems.size) { mediaItems[it].dateAddedSec }
        }
    }

    val totalGridItems = remember(gridTimestamps.size, gridState.layoutInfo.totalItemsCount) {
        if (gridTimestamps.isNotEmpty()) gridTimestamps.size
        else gridState.layoutInfo.totalItemsCount.coerceAtLeast(1)
    }

    // Smooth and accurate continuous scroll progress (0.0 at very top -> 1.0 at very bottom)
    val currentProgress by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val total = totalGridItems
            if (visibleItems.isEmpty() || total <= 1) {
                0f
            } else {
                val firstItem = visibleItems.first()
                val lastItem = visibleItems.last()
                val visibleCount = (lastItem.index - firstItem.index + 1).coerceAtLeast(1)
                val maxScrollIndex = (total - visibleCount).coerceAtLeast(1)

                val firstItemHeight = firstItem.size.height.toFloat().coerceAtLeast(1f)
                val scrollOffsetFraction = (gridState.firstVisibleItemScrollOffset.toFloat() / firstItemHeight).coerceIn(0f, 1f)
                val fractionalIndex = firstItem.index.toFloat() + scrollOffsetFraction
                (fractionalIndex / maxScrollIndex.toFloat()).coerceIn(0f, 1f)
            }
        }
    }

    // Drag offset tracking
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val effectiveProgress = if (isDragging) dragProgress else currentProgress

    // Exact Date text corresponding to the current visible section/row on screen or drag position
    val currentDateText by remember {
        derivedStateOf {
            if (gridTimestamps.isEmpty()) return@derivedStateOf ""

            val timestampSec: Long = if (isDragging) {
                val layoutInfo = gridState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                val visibleCount = if (visibleItems.isNotEmpty()) {
                    (visibleItems.last().index - visibleItems.first().index + 1).coerceAtLeast(1)
                } else 1
                val maxScrollIndex = (totalGridItems - visibleCount).coerceAtLeast(1)
                val targetIndex = (dragProgress * maxScrollIndex).roundToInt()
                    .coerceIn(0, gridTimestamps.size - 1)
                gridTimestamps[targetIndex]
            } else {
                val visibleItems = gridState.layoutInfo.visibleItemsInfo
                val topmostIndex = if (visibleItems.isNotEmpty()) {
                    val firstVisible = visibleItems.firstOrNull { it.offset.y + it.size.height > 0 } ?: visibleItems.first()
                    firstVisible.index.coerceIn(0, gridTimestamps.size - 1)
                } else {
                    gridState.firstVisibleItemIndex.coerceIn(0, (gridTimestamps.size - 1).coerceAtLeast(0))
                }
                gridTimestamps[topmostIndex]
            }

            if (timestampSec == 0L) ""
            else when (gridLevel) {
                GalleryGridLevel.Year -> FormatUtils.formatYearHeader(timestampSec)
                GalleryGridLevel.Month, GalleryGridLevel.Large, GalleryGridLevel.Medium -> FormatUtils.formatMonthHeader(timestampSec)
            }
        }
    }

    // Trigger haptic tick on date/month boundary change during scroll or drag
    LaunchedEffect(currentDateText) {
        if (currentDateText.isNotBlank() && currentDateText != lastMonthText) {
            if (lastMonthText.isNotBlank() && (isDragging || gridState.isScrollInProgress)) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            lastMonthText = currentDateText
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f),
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 24.dp, horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .onGloballyPositioned { coordinates ->
                    trackHeightPx = coordinates.size.height.toFloat()
                },
            contentAlignment = Alignment.TopEnd
        ) {
            val maxScrollTravelPx = (trackHeightPx - handleSizePx).coerceAtLeast(0f)

            // 1. Sleek vertical track guideline matching theme accent
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(3.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(primaryColor.copy(alpha = 0.20f))
            )

            // 2. Continuous Progress Fill Indicator
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset {
                        val currentY = (effectiveProgress * maxScrollTravelPx).roundToInt()
                        IntOffset(x = 0, y = currentY.coerceIn(0, maxScrollTravelPx.roundToInt()))
                    }
                    .width(3.5.dp)
                    .height(handleSizeDp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(primaryColor)
            )

            // 3. Interactive Floating Drag Handle & Floating Date Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .offset {
                        val currentY = (effectiveProgress * maxScrollTravelPx).roundToInt()
                        IntOffset(x = 0, y = currentY.coerceIn(0, maxScrollTravelPx.roundToInt()))
                    }
                    .pointerInput(totalGridItems, maxScrollTravelPx) {
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                if (maxScrollTravelPx > 0f) {
                                    val startY = offset.y + (effectiveProgress * maxScrollTravelPx)
                                    dragProgress = (startY / maxScrollTravelPx).coerceIn(0f, 1f)
                                }
                            },
                            onDragEnd = {
                                isDragging = false
                            },
                            onDragCancel = {
                                isDragging = false
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                if (maxScrollTravelPx > 0f && totalGridItems > 0) {
                                    val deltaProgress = dragAmount / maxScrollTravelPx
                                    dragProgress = (dragProgress + deltaProgress).coerceIn(0f, 1f)

                                    val visibleItems = gridState.layoutInfo.visibleItemsInfo
                                    val visibleCount = if (visibleItems.isNotEmpty()) {
                                        (visibleItems.last().index - visibleItems.first().index + 1).coerceAtLeast(1)
                                    } else 1
                                    val maxScrollIndex = (totalGridItems - visibleCount).coerceAtLeast(1)
                                    val targetIndex = (dragProgress * maxScrollIndex).roundToInt()
                                        .coerceIn(0, totalGridItems - 1)

                                    scrollJob?.cancel()
                                    scrollJob = scope.launch {
                                        gridState.scrollToItem(targetIndex, 0)
                                    }
                                }
                            }
                        )
                    }
            ) {
                // Floating Date Pill (Google Photos Style)
                AnimatedVisibility(
                    visible = (isDragging || gridState.isScrollInProgress) && currentDateText.isNotBlank(),
                    enter = fadeIn() + scaleIn(initialScale = 0.85f),
                    exit = fadeOut() + scaleOut(targetScale = 0.85f)
                ) {
                    Surface(
                        color = if (isDark) Color(0xFF2C2F36) else Color(0xFF373B44),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = 6.dp,
                        modifier = Modifier.padding(end = 10.dp)
                    ) {
                        Text(
                            text = currentDateText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }

                // Smooth Drag Handle Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(handleSizeDp)
                        .shadow(elevation = if (isDragging) 8.dp else 4.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(
                            if (isDragging) primaryColor else if (isDark) Color(0xFF434750) else Color(0xFF5A5E6B)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.UnfoldMore,
                        contentDescription = "Timeline Fast Scroll",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
