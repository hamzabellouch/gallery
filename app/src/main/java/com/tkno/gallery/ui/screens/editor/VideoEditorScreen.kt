package com.tkno.gallery.ui.screens.editor

import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.tkno.gallery.data.model.MediaItem
import com.tkno.gallery.util.FormatUtils
import com.tkno.gallery.util.VideoEditorUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun VideoEditorScreen(
    mediaItem: MediaItem,
    onNavigateBack: () -> Unit,
    onSaveSuccess: (Uri) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var isCropMode by remember { mutableStateOf(false) }
    var cropRectNormalized by remember { mutableStateOf<RectF?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Video Duration & Trim State
    var totalDurationMs by remember { mutableLongStateOf(mediaItem.durationMs.coerceAtLeast(1000L)) }
    var trimStartMs by remember { mutableLongStateOf(0L) }
    var trimEndMs by remember { mutableLongStateOf(totalDurationMs) }
    var currentPlaybackPositionMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }

    val hasChanges = remember(rotationAngle, trimStartMs, trimEndMs, totalDurationMs, cropRectNormalized) {
        val normalizedAngle = ((rotationAngle % 360f) + 360f) % 360f
        normalizedAngle != 0f || trimStartMs > 500L || (totalDurationMs - trimEndMs > 500L) || cropRectNormalized != null
    }

    val animatedRotation by animateFloatAsState(
        targetValue = rotationAngle,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "videoRotationAnimation"
    )

    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var videoIntrinsicSize by remember {
        mutableStateOf(
            if (mediaItem.width > 0 && mediaItem.height > 0)
                Size(mediaItem.width.toFloat(), mediaItem.height.toFloat())
            else Size(1080f, 1920f)
        )
    }

    val targetScale by remember(canvasSize, videoIntrinsicSize, rotationAngle) {
        derivedStateOf {
            if (canvasSize.width <= 0f || canvasSize.height <= 0f || videoIntrinsicSize.width <= 0f || videoIntrinsicSize.height <= 0f) {
                1f
            } else {
                val cw = canvasSize.width
                val ch = canvasSize.height
                val iw = videoIntrinsicSize.width
                val ih = videoIntrinsicSize.height

                val scaleFit = kotlin.math.min(cw / iw, ch / ih)
                val baseW = iw * scaleFit
                val baseH = ih * scaleFit

                val rad = Math.toRadians(rotationAngle.toDouble())
                val cos = kotlin.math.abs(kotlin.math.cos(rad)).toFloat()
                val sin = kotlin.math.abs(kotlin.math.sin(rad)).toFloat()

                val rotatedBoundingW = baseW * cos + baseH * sin
                val rotatedBoundingH = baseW * sin + baseH * cos

                if (rotatedBoundingW > cw || rotatedBoundingH > ch) {
                    kotlin.math.min(cw / rotatedBoundingW, ch / rotatedBoundingH)
                } else {
                    1f
                }
            }
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "videoScaleAnimation"
    )

    // Load timeline preview thumbnails
    var timelineThumbnails by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    LaunchedEffect(mediaItem.uri) {
        timelineThumbnails = VideoEditorUtils.extractVideoThumbnails(
            context = context,
            videoUri = mediaItem.uri,
            durationMs = totalDurationMs,
            frameCount = 8
        )
    }

    // ExoPlayer initialization
    val player = remember(context, mediaItem.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(mediaItem.uri))
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = false
            prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val dur = player.duration
                    if (dur > 0 && dur != totalDurationMs) {
                        totalDurationMs = dur
                        if (trimEndMs == 0L || trimEndMs >= dur || trimEndMs == mediaItem.durationMs) {
                            trimEndMs = dur
                        }
                    }
                    if (player.videoSize.width > 0 && player.videoSize.height > 0) {
                        videoIntrinsicSize = Size(player.videoSize.width.toFloat(), player.videoSize.height.toFloat())
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Loop within [trimStartMs, trimEndMs]
    LaunchedEffect(player, trimStartMs, trimEndMs, isPlaying) {
        while (true) {
            if (player.isPlaying) {
                val pos = player.currentPosition
                if (pos >= trimEndMs) {
                    player.seekTo(trimStartMs)
                } else if (pos < trimStartMs) {
                    player.seekTo(trimStartMs)
                }
                currentPlaybackPositionMs = player.currentPosition
            }
            delay(35)
        }
    }

    BackHandler {
        if (isCropMode) {
            isCropMode = false
        } else if (hasChanges) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Video Canvas Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .onGloballyPositioned { coordinates ->
                        canvasSize = coordinates.size.toSize()
                    },
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = animatedRotation
                            scaleX = animatedScale
                            scaleY = animatedScale
                        }
                        .clickable {
                            if (player.isPlaying) player.pause() else player.play()
                        }
                )

                // Play / Pause Overlay Button (Center of Video)
                if (!isPlaying) {
                    FilledIconButton(
                        onClick = {
                            if (player.currentPosition >= trimEndMs || player.currentPosition < trimStartMs) {
                                player.seekTo(trimStartMs)
                            }
                            player.play()
                        },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.65f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(68.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            // Action Buttons (Rotate & Crop) - Pill Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Rotate Button
                Surface(
                    onClick = {
                        rotationAngle = (rotationAngle - 90f) % 360f
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E1E1E),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                            contentDescription = "Rotate",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Rotate",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Crop Button
                Surface(
                    onClick = {
                        isCropMode = !isCropMode
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E1E1E),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Crop,
                            contentDescription = "Crop",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Crop",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Video Trimmer Timeline Strip (Matching Reference Screenshot)
            VideoTimelineTrimmer(
                thumbnails = timelineThumbnails,
                totalDurationMs = totalDurationMs,
                trimStartMs = trimStartMs,
                trimEndMs = trimEndMs,
                currentPositionMs = currentPlaybackPositionMs,
                onTrimChanged = { newStart, newEnd ->
                    player.pause()
                    trimStartMs = newStart
                    trimEndMs = newEnd
                    player.seekTo(newStart)
                    currentPlaybackPositionMs = newStart
                },
                onSeek = { seekPos ->
                    player.pause()
                    player.seekTo(seekPos)
                    currentPlaybackPositionMs = seekPos
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Action Bar (Cancel & Save copy)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel Button
                TextButton(
                    onClick = {
                        if (hasChanges) {
                            showDiscardDialog = true
                        } else {
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                // Save Copy Button
                Button(
                    onClick = {
                        if (!hasChanges || isSaving) return@Button
                        isSaving = true
                        player.pause()
                        coroutineScope.launch {
                            val savedUri = VideoEditorUtils.trimVideo(
                                context = context,
                                sourceUri = mediaItem.uri,
                                originalName = mediaItem.name,
                                startMs = trimStartMs,
                                endMs = trimEndMs,
                                rotationAngle = rotationAngle
                            )
                            isSaving = false
                            if (savedUri != null) {
                                Toast.makeText(context, "Saved copy to Gallery", Toast.LENGTH_SHORT).show()
                                onSaveSuccess(savedUri)
                            } else {
                                Toast.makeText(context, "Failed to save video", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = hasChanges && !isSaving,
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFA8C7FA),
                        contentColor = Color(0xFF062E6F),
                        disabledContainerColor = Color(0xFF1E1E1E),
                        disabledContentColor = Color.White.copy(alpha = 0.35f)
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFF062E6F),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "Save copy",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Discard Changes Dialog
        if (showDiscardDialog) {
            VideoDiscardChangesDialog(
                onKeepEditing = { showDiscardDialog = false },
                onDiscard = {
                    showDiscardDialog = false
                    onNavigateBack()
                }
            )
        }
    }
}

@Composable
private fun VideoTimelineTrimmer(
    thumbnails: List<Bitmap>,
    totalDurationMs: Long,
    trimStartMs: Long,
    trimEndMs: Long,
    currentPositionMs: Long,
    onTrimChanged: (startMs: Long, endMs: Long) -> Unit,
    onSeek: (seekPosMs: Long) -> Unit
) {
    var stripWidthPx by remember { mutableFloatStateOf(0f) }
    val handleWidthDp = 18.dp
    val density = LocalDensity.current
    val handleWidthPx = with(density) { handleWidthDp.toPx() }

    val startFraction = if (totalDurationMs > 0) (trimStartMs.toFloat() / totalDurationMs).coerceIn(0f, 1f) else 0f
    val endFraction = if (totalDurationMs > 0) (trimEndMs.toFloat() / totalDurationMs).coerceIn(0f, 1f) else 1f
    val progressFraction = if (totalDurationMs > 0) (currentPositionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f) else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Duration range label
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = FormatUtils.formatDuration(trimStartMs),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Trimmed: ${FormatUtils.formatDuration((trimEndMs - trimStartMs).coerceAtLeast(0))}",
                fontSize = 12.sp,
                color = Color(0xFFA8C7FA),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = FormatUtils.formatDuration(trimEndMs),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }

        // Filmstrip Timeline Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A1A1A))
                .onGloballyPositioned { coordinates ->
                    stripWidthPx = coordinates.size.width.toFloat()
                }
        ) {
            // 1. Thumbnails row
            if (thumbnails.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    thumbnails.forEach { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF242424))
                )
            }

            if (stripWidthPx > 0f) {
                val leftHandleX = (startFraction * (stripWidthPx - 2 * handleWidthPx)).coerceAtLeast(0f)
                val rightHandleX = (endFraction * (stripWidthPx - 2 * handleWidthPx) + handleWidthPx).coerceIn(leftHandleX + handleWidthPx, stripWidthPx - handleWidthPx)

                // 2. Dimmed Overlay outside trim window
                // Left dimmed area
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(with(density) { leftHandleX.toDp() })
                        .background(Color.Black.copy(alpha = 0.65f))
                        .align(Alignment.CenterStart)
                )

                // Right dimmed area
                val rightDimWidth = (stripWidthPx - (rightHandleX + handleWidthPx)).coerceAtLeast(0f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(with(density) { rightDimWidth.toDp() })
                        .background(Color.Black.copy(alpha = 0.65f))
                        .align(Alignment.CenterEnd)
                )

                // 3. Top and Bottom Active Frame Border
                val selectedWidth = (rightHandleX + handleWidthPx - leftHandleX).coerceAtLeast(0f)
                Box(
                    modifier = Modifier
                        .offset(x = with(density) { leftHandleX.toDp() })
                        .width(with(density) { selectedWidth.toDp() })
                        .fillMaxHeight()
                        .border(
                            border = BorderStroke(2.5.dp, Color.White),
                            shape = RoundedCornerShape(6.dp)
                        )
                )

                // 4. Playhead indicator line
                val playheadX = (progressFraction * (stripWidthPx - 2f)).coerceIn(leftHandleX, rightHandleX + handleWidthPx)
                Box(
                    modifier = Modifier
                        .offset(x = with(density) { playheadX.toDp() })
                        .width(2.5.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFA8C7FA))
                )

                // 5. Left Handle (White vertical pill with 3 dots)
                Box(
                    modifier = Modifier
                        .offset(x = with(density) { leftHandleX.toDp() })
                        .width(handleWidthDp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                        .background(Color.White)
                        .pointerInput(totalDurationMs, stripWidthPx) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val usableWidth = (stripWidthPx - 2 * handleWidthPx).coerceAtLeast(1f)
                                val currentX = startFraction * usableWidth
                                val newX = (currentX + dragAmount.x).coerceIn(0f, rightHandleX - handleWidthPx)
                                val newStartFrac = (newX / usableWidth).coerceIn(0f, endFraction - 0.05f)
                                val newStartMs = (newStartFrac * totalDurationMs).toLong()
                                onTrimChanged(newStartMs, trimEndMs)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                            )
                        }
                    }
                }

                // 6. Right Handle (White vertical pill with 3 dots)
                Box(
                    modifier = Modifier
                        .offset(x = with(density) { rightHandleX.toDp() })
                        .width(handleWidthDp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                        .background(Color.White)
                        .pointerInput(totalDurationMs, stripWidthPx) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val usableWidth = (stripWidthPx - 2 * handleWidthPx).coerceAtLeast(1f)
                                val currentX = (endFraction * usableWidth) + handleWidthPx
                                val newX = (currentX + dragAmount.x).coerceIn(leftHandleX + 2 * handleWidthPx, stripWidthPx)
                                val newEndFrac = ((newX - handleWidthPx) / usableWidth).coerceIn(startFraction + 0.05f, 1f)
                                val newEndMs = (newEndFrac * totalDurationMs).toLong()
                                onTrimChanged(trimStartMs, newEndMs)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoDiscardChangesDialog(
    onKeepEditing: () -> Unit,
    onDiscard: () -> Unit
) {
    Dialog(onDismissRequest = onKeepEditing) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF2C2D31),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Discard changes?",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Your changes won't be saved",
                    color = Color(0xFFC4C7C5),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(26.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Keep Editing button
                    Surface(
                        onClick = onKeepEditing,
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF383A42),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Keep Editing",
                                color = Color(0xFFA8C7FA),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Discard button
                    Surface(
                        onClick = onDiscard,
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFA8C7FA),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Discard",
                                color = Color(0xFF062E6F),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
