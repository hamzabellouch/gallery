package com.tkno.gallery.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.tkno.gallery.data.model.MediaItem
import com.tkno.gallery.data.model.ResolutionBadge
import com.tkno.gallery.util.FormatUtils

private val overlayGradient = Brush.verticalGradient(
    colors = listOf(
        Color.Black.copy(alpha = 0.35f),
        Color.Transparent,
        Color.Black.copy(alpha = 0.65f)
    )
)

private val shapeYear = RoundedCornerShape(1.dp)
private val shapeMonth = RoundedCornerShape(3.dp)
private val shapeDefault = RoundedCornerShape(8.dp)

private val paddingYear = 0.5.dp
private val paddingMonth = 1.dp
private val paddingDefault = 2.dp

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun MediaGridItem(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    columnCount: Int = 3,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    showResolutionBadge: Boolean = true,
    roundedCorners: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val context = LocalContext.current

    val shape = if (roundedCorners) {
        when {
            columnCount >= 10 -> shapeYear
            columnCount >= 7 -> shapeMonth
            else -> shapeDefault
        }
    } else {
        RectangleShape
    }

    val paddingDp = when {
        columnCount >= 10 -> paddingYear
        columnCount >= 7 -> paddingMonth
        else -> paddingDefault
    }

    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)

    var boxModifier = modifier
        .aspectRatio(1f)
        .padding(paddingDp)
        .clip(shape)
        .background(placeholderColor)

    if (isSelectionMode && isSelected) {
        boxModifier = boxModifier.border(
            width = if (columnCount >= 7) 1.5.dp else 2.5.dp,
            color = MaterialTheme.colorScheme.primary,
            shape = shape
        )
    }

    // Crucial: Shared element transitions are only enabled in Day views (<= 4 columns)
    // to eliminate massive layout coordinate tracking overhead across 100-200 items during Month/Year zoom scroll
    if (columnCount <= 4 && sharedTransitionScope != null && animatedVisibilityScope != null && !isSelectionMode) {
        with(sharedTransitionScope) {
            boxModifier = boxModifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = "media_${item.id}"),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ ->
                    spring(
                        dampingRatio = 0.82f,
                        stiffness = 380f
                    )
                }
            )
        }
    }

    // Adaptive thumbnail sizing mapped directly to Android hardware thumbnail cache tiers
    val targetThumbnailPx = when {
        columnCount >= 10 -> 96     // Android MICRO_KIND hardware thumbnail cache
        columnCount >= 7 -> 160    // Month view
        columnCount == 4 -> 256    // Medium view
        else -> 384                // Large view
    }

    val imageRequest = remember(item.uri, targetThumbnailPx) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .size(targetThumbnailPx)
            .precision(Precision.INEXACT)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    Box(
        modifier = boxModifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    ) {
        // High-Speed Ultra-Smooth Grid Thumbnail Request
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dim overlay when selected in selection mode
        if (isSelectionMode && isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
            )
        }

        // Selection Checkmark Overlay
        if (isSelectionMode) {
            val checkSize = when {
                columnCount >= 10 -> 14.dp
                columnCount >= 7 -> 18.dp
                else -> 24.dp
            }
            val iconSize = when {
                columnCount >= 10 -> 10.dp
                columnCount >= 7 -> 12.dp
                else -> 16.dp
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(if (columnCount >= 7) 3.dp else 6.dp)
                    .size(checkSize)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Black.copy(alpha = 0.45f)
                    )
                    .then(
                        if (!isSelected) Modifier.border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }

        // Overlays and badges are only shown on larger zoom levels (columns <= 4)
        if (columnCount <= 4 && !isSelectionMode) {
            val badge = item.resolutionBadge

            // Gradient overlay for text legibility
            if (item.isVideo || (showResolutionBadge && badge != ResolutionBadge.NONE)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(overlayGradient)
                )
            }

            // Top-Right Resolution Badge (8K, 4K, 2K, Full HD, HD)
            if (showResolutionBadge && badge != ResolutionBadge.NONE) {
                Icon(
                    painter = painterResource(id = badge.iconResId),
                    contentDescription = "${badge.label} Media",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(if (columnCount == 4) 3.dp else 4.dp)
                        .size(if (columnCount == 4) 18.dp else 22.dp)
                )
            }

            // Bottom Video Badge with Play icon and Duration in a compact capsule
            if (item.isVideo) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(if (columnCount == 4) 3.dp else 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = if (columnCount == 4) 3.5.dp else 4.5.dp,
                            vertical = 1.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(if (columnCount == 4) 8.5.dp else 10.dp)
                        )
                        Spacer(modifier = Modifier.width(1.5.dp))
                        Text(
                            text = FormatUtils.formatDuration(item.durationMs),
                            color = Color.White,
                            fontSize = if (columnCount == 4) 8.sp else 9.5.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        } else if (columnCount == 7 && item.isVideo && !isSelectionMode) {
            // Minimal video icon capsule for month view
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(1.5.dp)
            ) {
                Box(
                    modifier = Modifier.padding(1.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(7.dp)
                    )
                }
            }
        }
    }
}
