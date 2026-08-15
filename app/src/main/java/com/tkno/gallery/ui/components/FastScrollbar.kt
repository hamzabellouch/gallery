package com.tkno.gallery.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.tkno.gallery.util.FormatUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * High-performance Dual FastScrollbar:
 * 1. Global Indicator Bar: A slim bar matching the active tab theme color (Gallery / Albums)
 *    anchored to the screen edge that displays the true overall progress (0% at top, 50% in middle, 100% at end)
 *    with the exact height of the circle (37.dp).
 * 2. Cyclic Handle: A circular drag button (37.dp) that cycles smoothly through 50 rows per stroke.
 */
@Composable
fun FastScrollbar(
    gridState: LazyGridState,
    mediaItems: List<MediaItem>,
    modifier: Modifier = Modifier,
    columnCount: Int = 3,
    rowsPerCycle: Int = 50,
    accentColor: Color? = null
) {
    if (mediaItems.isEmpty()) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // Resolve matching tab/theme active color
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
    var showDatePill by remember { mutableStateOf(false) }
    var trackHeightPx by remember { mutableFloatStateOf(0f) }
    val handleSizeDp = 37.dp
    val handleSizePx = remember(density) { with(density) { handleSizeDp.toPx() } }

    var scrollJob by remember { mutableStateOf<Job?>(null) }

    // Auto-hide logic: Stay visible during scroll/drag and fade out after 2.5 seconds
    LaunchedEffect(gridState.isScrollInProgress, isDragging) {
        if (gridState.isScrollInProgress || isDragging) {
            isVisible = true
        } else {
            delay(2500)
            isVisible = false
        }
    }

    val totalGridItems = remember(gridState.layoutInfo.totalItemsCount, mediaItems.size) {
        gridState.layoutInfo.totalItemsCount.coerceAtLeast(mediaItems.size)
    }

    val firstVisibleIndex by remember {
        derivedStateOf { gridState.firstVisibleItemIndex }
    }
    val firstVisibleOffset by remember {
        derivedStateOf { gridState.firstVisibleItemScrollOffset }
    }

    // Global Progress Fraction: Absolute progress across the entire media dataset (0.0 to 1.0)
    val globalProgressFraction by remember {
        derivedStateOf {
            val total = totalGridItems.coerceAtLeast(1)
            if (total <= 1) 0f
            else (firstVisibleIndex.toFloat() / (total - 1).toFloat()).coerceIn(0f, 1f)
        }
    }

    // Calculate current row within the 50-row cycle
    val safeCols = columnCount.coerceAtLeast(1)
    val safeRowsPerCycle = rowsPerCycle.coerceAtLeast(1)
    val itemsPerCycle = safeRowsPerCycle * safeCols

    val cycleFraction by remember {
        derivedStateOf {
            val approximateRow = firstVisibleIndex / safeCols
            val cycleRow = approximateRow % safeRowsPerCycle
            // Approximate fractional progress within the current row
            val rowFraction = (firstVisibleOffset / 800f).coerceIn(0f, 0.99f)
            ((cycleRow + rowFraction) / safeRowsPerCycle.toFloat()).coerceIn(0f, 1f)
        }
    }

    // Current Date text (e.g. "Feb 2025")
    val currentDateText by remember {
        derivedStateOf {
            val clampedIndex = firstVisibleIndex.coerceIn(0, (mediaItems.size - 1).coerceAtLeast(0))
            if (mediaItems.isNotEmpty()) {
                FormatUtils.formatMonthYear(mediaItems[clampedIndex].dateAddedSec)
            } else {
                ""
            }
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(initialScale = 0.85f),
        exit = fadeOut() + scaleOut(targetScale = 0.85f),
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

            // 1. Faint vertical edge guideline matching theme accent
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(3.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(primaryColor.copy(alpha = 0.22f))
            )

            // 2. Global Progress Indicator Bar (Follows true global position, colored with Gallery/Albums theme)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset {
                        val globalY = (globalProgressFraction * maxScrollTravelPx).roundToInt()
                        IntOffset(x = 0, y = globalY.coerceIn(0, maxScrollTravelPx.roundToInt()))
                    }
                    .width(3.5.dp)
                    .height(handleSizeDp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(primaryColor)
            )

            // 3. Cyclic Drag Handle Row (Circular Button + Floating Date Pill)
            var dragStartItemIndex by remember { mutableIntStateOf(0) }
            var dragAccumulatedPx by remember { mutableFloatStateOf(0f) }
            var lastCycleCrossed by remember { mutableIntStateOf(0) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .offset {
                        val currentY = (cycleFraction * maxScrollTravelPx).roundToInt()
                        IntOffset(x = 0, y = currentY.coerceIn(0, maxScrollTravelPx.roundToInt()))
                    }
                    .pointerInput(safeCols, safeRowsPerCycle, trackHeightPx) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                isDragging = true
                                dragStartItemIndex = gridState.firstVisibleItemIndex
                                dragAccumulatedPx = 0f
                                lastCycleCrossed = dragStartItemIndex / itemsPerCycle
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
                                    dragAccumulatedPx += dragAmount
                                    val itemsPerPx = itemsPerCycle.toFloat() / maxScrollTravelPx
                                    val deltaItems = (dragAccumulatedPx * itemsPerPx).toInt()
                                    val targetIndex = (dragStartItemIndex + deltaItems)
                                        .coerceIn(0, totalGridItems - 1)

                                    val currentCycle = targetIndex / itemsPerCycle
                                    if (currentCycle != lastCycleCrossed) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        lastCycleCrossed = currentCycle
                                    }

                                    scrollJob?.cancel()
                                    scrollJob = scope.launch {
                                        gridState.scrollToItem(targetIndex)
                                    }
                                }
                            }
                        )
                    }
            ) {
                // Floating Date Pill (Visible during drag or when toggled / scrolling)
                AnimatedVisibility(
                    visible = (isDragging || showDatePill || gridState.isScrollInProgress) && currentDateText.isNotBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        color = Color(0xFF2C2F36),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(18.dp),
                        shadowElevation = 4.dp,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = currentDateText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Cyclic Fast Scroll Drag Handle Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(handleSizeDp)
                        .shadow(elevation = 4.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFF434750))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showDatePill = !showDatePill
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.UnfoldMore,
                        contentDescription = "Fast Scroll (50 rows per cycle)",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}
