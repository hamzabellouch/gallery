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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tkno.gallery.data.model.MediaItem
import com.tkno.gallery.theme.TaskbarActivePrimaryDark
import com.tkno.gallery.theme.TaskbarActivePrimaryLight
import com.tkno.gallery.ui.screens.home.GalleryGridLevel
import com.tkno.gallery.util.FormatUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Ultra-Smooth Google Photos-Style Timeline FastScrollbar:
 *
 * 1. Zero-Recomposition Scrolling: Progress is read exclusively in graphicsLayer Draw phase on the GPU.
 * 2. Rock-Solid Index Synchronization: Non-oscillating formula prevents jumps or flutter across date headers.
 * 3. Frame-Synced Flow Dispatcher: Deduplicates redundant scroll events to guarantee 60-120fps fluid scrolling.
 * 4. Seamless Drag-to-Rest Handoff: Eliminates snap-back and sudden position jerks when stopping anywhere.
 * 5. Isolated Floating Date Pill: Updates only when the month/year header string changes.
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

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

    val maxScrollIndex = remember(totalGridItems) {
        (totalGridItems - 1).coerceAtLeast(1)
    }

    // Drag offset tracking
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val targetIndexState = remember { mutableIntStateOf(0) }

    // High-efficiency, Frame-Synced Scroll Dispatcher without Job-cancelling thrash
    LaunchedEffect(isDragging) {
        if (isDragging) {
            snapshotFlow { targetIndexState.intValue }
                .distinctUntilChanged()
                .collect { targetIndex ->
                    gridState.scrollToItem(targetIndex, 0)
                }
        }
    }

    // Exact Date text corresponding to the current visible section/row on screen or drag position
    val currentDateText by remember(gridTimestamps, gridLevel) {
        derivedStateOf {
            if (gridTimestamps.isEmpty()) return@derivedStateOf ""

            val timestampSec: Long = if (isDragging) {
                val targetIndex = (dragProgress * maxScrollIndex).roundToInt()
                    .coerceIn(0, gridTimestamps.size - 1)
                gridTimestamps[targetIndex]
            } else {
                val topmostIndex = gridState.firstVisibleItemIndex.coerceIn(0, (gridTimestamps.size - 1).coerceAtLeast(0))
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
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 56.dp, bottom = 80.dp, start = 4.dp, end = 4.dp)
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

            // 2. Continuous Progress Fill Indicator (GPU GraphicsLayer Translated with ZERO recompositions)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .graphicsLayer {
                        val prog = if (isDragging) {
                            dragProgress
                        } else {
                            if (totalGridItems <= 1) 0f
                            else (gridState.firstVisibleItemIndex.toFloat() / maxScrollIndex.toFloat()).coerceIn(0f, 1f)
                        }
                        translationY = (prog * maxScrollTravelPx).coerceIn(0f, maxScrollTravelPx)
                    }
                    .width(3.5.dp)
                    .height(handleSizeDp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(primaryColor)
            )

            // 3. Interactive Floating Drag Handle & Floating Date Pill (GPU GraphicsLayer Translated)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .graphicsLayer {
                        val prog = if (isDragging) {
                            dragProgress
                        } else {
                            if (totalGridItems <= 1) 0f
                            else (gridState.firstVisibleItemIndex.toFloat() / maxScrollIndex.toFloat()).coerceIn(0f, 1f)
                        }
                        translationY = (prog * maxScrollTravelPx).coerceIn(0f, maxScrollTravelPx)
                    }
                    .pointerInput(totalGridItems, maxScrollTravelPx) {
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                if (maxScrollTravelPx > 0f) {
                                    val currentProg = if (totalGridItems <= 1) 0f else (gridState.firstVisibleItemIndex.toFloat() / maxScrollIndex.toFloat()).coerceIn(0f, 1f)
                                    val startY = offset.y + (currentProg * maxScrollTravelPx)
                                    val startProg = (startY / maxScrollTravelPx).coerceIn(0f, 1f)
                                    dragProgress = startProg
                                    val targetIndex = (startProg * maxScrollIndex).roundToInt()
                                        .coerceIn(0, totalGridItems - 1)
                                    targetIndexState.intValue = targetIndex
                                }
                            },
                            onDragEnd = {
                                scope.launch {
                                    delay(50)
                                    isDragging = false
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                if (maxScrollTravelPx > 0f && totalGridItems > 0) {
                                    val deltaProgress = dragAmount / maxScrollTravelPx
                                    val newProgress = (dragProgress + deltaProgress).coerceIn(0f, 1f)
                                    dragProgress = newProgress

                                    val targetIndex = (newProgress * maxScrollIndex).roundToInt()
                                        .coerceIn(0, totalGridItems - 1)
                                    targetIndexState.intValue = targetIndex
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
                        color = if (isDark) Color(0xFF2C2F36) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = 6.dp,
                        modifier = Modifier.padding(end = 10.dp)
                    ) {
                        Text(
                            text = currentDateText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
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
                            if (isDragging) primaryColor else if (isDark) Color(0xFF434750) else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.UnfoldMore,
                        contentDescription = "Timeline Fast Scroll",
                        tint = if (isDragging) (if (isDark) Color.White else MaterialTheme.colorScheme.onPrimary) else if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
