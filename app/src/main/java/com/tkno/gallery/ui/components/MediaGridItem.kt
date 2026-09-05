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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun MediaGridItem(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    columnCount: Int = 3,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val context = LocalContext.current
    val badge = item.resolutionBadge

    val paddingDp = when {
        columnCount >= 10 -> 0.5.dp
        columnCount >= 7 -> 1.dp
        else -> 2.dp
    }

    val cornerRadiusDp = when {
        columnCount >= 10 -> 1.dp
        columnCount >= 7 -> 3.dp
        else -> 8.dp
    }

    val shape = remember(cornerRadiusDp) { RoundedCornerShape(cornerRadiusDp) }

    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

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

    if (sharedTransitionScope != null && animatedVisibilityScope != null && !isSelectionMode) {
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

    val imageRequest = remember(item.uri, columnCount) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .size(if (columnCount >= 7) 180 else 256)
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

        // Only show overlays and badges on larger zoom levels (columns <= 4)
        if (columnCount <= 4 && !isSelectionMode) {
            // Gradient overlay for text legibility
            if (item.isVideo || badge != ResolutionBadge.NONE) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(overlayGradient)
                )
            }

            // Top-Right Resolution Badge (8K, 4K, 2K, Full HD, HD)
            if (badge != ResolutionBadge.NONE) {
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

            // Bottom Video Badge with Play icon and Duration
            if (item.isVideo) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(if (columnCount == 4) 4.dp else 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(if (columnCount == 4) 14.dp else 16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = FormatUtils.formatDuration(item.durationMs),
                        color = Color.White,
                        fontSize = if (columnCount == 4) 9.sp else 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else if (columnCount == 7 && item.isVideo && !isSelectionMode) {
            // Minimal video icon for month view
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
