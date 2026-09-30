package com.tkno.gallery.ui.screens.editor

import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.tkno.gallery.R
import com.tkno.gallery.data.model.MediaItem
import com.tkno.gallery.ui.components.CustomIcons
import com.tkno.gallery.util.FormatUtils
import com.tkno.gallery.util.VideoEditorUtils
import com.tkno.gallery.util.VideoEngineManager
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
    val density = LocalDensity.current

    var isCropMode by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Video Crop Working State
    var cropLeft by remember { mutableFloatStateOf(0f) }
    var cropTop by remember { mutableFloatStateOf(0f) }
    var cropRight by remember { mutableFloatStateOf(1f) }
    var cropBottom by remember { mutableFloatStateOf(1f) }
    var selectedAspectRatio by remember { mutableStateOf("Free") }

    // Video Duration & Playback State
    var totalDurationMs by remember { mutableLongStateOf(mediaItem.durationMs.coerceAtLeast(1000L)) }
    var currentPlaybackPositionMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }

    // Hardened ExoPlayer initialization
    val player = remember(context, mediaItem.uri) {
        VideoEngineManager.createHardenedPlayer(context).apply {
            setMediaItem(ExoMediaItem.fromUri(mediaItem.uri))
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = false
            prepare()
        }
    }

    // History stack for Undo / Redo
    val initialDuration = totalDurationMs
    val history = remember {
        mutableStateListOf(
            VideoEditorHistoryState(
                rotationAngle = 0f,
                cropRectNormalized = null,
                trimStartMs = 0L,
                trimEndMs = initialDuration
            )
        )
    }
    var historyIndex by remember { mutableIntStateOf(0) }

    val currentState = history.getOrElse(historyIndex) {
        VideoEditorHistoryState(trimEndMs = initialDuration)
    }
    val rotationAngle = currentState.rotationAngle
    val cropRectNormalized = currentState.cropRectNormalized

    // Active / in-progress trim values (syncs with currentState, but updates smoothly during drag)
    var activeTrimStartMs by remember { mutableLongStateOf(0L) }
    var activeTrimEndMs by remember { mutableLongStateOf(initialDuration) }

    LaunchedEffect(currentState) {
        activeTrimStartMs = currentState.trimStartMs
        activeTrimEndMs = currentState.trimEndMs
    }

    val canUndo = historyIndex > 0
    val canRedo = historyIndex < history.size - 1

    fun pushState(newState: VideoEditorHistoryState) {
        if (newState == currentState) return
        while (history.size > historyIndex + 1) {
            history.removeAt(history.size - 1)
        }
        history.add(newState)
        historyIndex = history.size - 1
    }

    fun undo() {
        if (canUndo) {
            historyIndex--
            player.pause()
            val target = history[historyIndex]
            player.seekTo(target.trimStartMs)
            currentPlaybackPositionMs = target.trimStartMs
        }
    }

    fun redo() {
        if (canRedo) {
            historyIndex++
            player.pause()
            val target = history[historyIndex]
            player.seekTo(target.trimStartMs)
            currentPlaybackPositionMs = target.trimStartMs
        }
    }

    val hasChanges = remember(rotationAngle, activeTrimStartMs, activeTrimEndMs, totalDurationMs, cropRectNormalized) {
        val normalizedAngle = ((rotationAngle % 360f) + 360f) % 360f
        normalizedAngle != 0f || activeTrimStartMs > 500L || (totalDurationMs - activeTrimEndMs > 500L) || cropRectNormalized != null
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

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val dur = player.duration
                    if (dur > 0 && dur != totalDurationMs) {
                        val oldDur = totalDurationMs
                        totalDurationMs = dur
                        if (history.size == 1 && history[0].trimEndMs == oldDur) {
                            history[0] = history[0].copy(trimEndMs = dur)
                            activeTrimEndMs = dur
                        }
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    player.pause()
                    player.seekTo(activeTrimStartMs)
                    currentPlaybackPositionMs = activeTrimStartMs
                    isPlaying = false
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val isExoRotated = videoSize.unappliedRotationDegrees == 90 || videoSize.unappliedRotationDegrees == 270
                    val realW = if (isExoRotated) videoSize.height.toFloat() else videoSize.width.toFloat()
                    val realH = if (isExoRotated) videoSize.width.toFloat() else videoSize.height.toFloat()
                    videoIntrinsicSize = Size(realW, realH)
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.stop()
            player.clearMediaItems()
            player.release()
        }
    }

    // Play / Pause Toggle Logic with safe position bounds
    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            val cur = player.currentPosition
            if (cur >= activeTrimEndMs - 100 || cur < activeTrimStartMs || player.playbackState == Player.STATE_ENDED) {
                player.seekTo(activeTrimStartMs)
                currentPlaybackPositionMs = activeTrimStartMs
            }
            player.play()
        }
    }

    // Playback progress tracking
    LaunchedEffect(isPlaying, activeTrimStartMs, activeTrimEndMs) {
        if (!isPlaying) return@LaunchedEffect
        delay(50) // Small delay to let initial seek settle
        while (isPlaying) {
            val pos = player.currentPosition
            if (pos >= activeTrimEndMs) {
                player.pause()
                player.seekTo(activeTrimStartMs)
                currentPlaybackPositionMs = activeTrimStartMs
                break
            } else {
                currentPlaybackPositionMs = pos
            }
            delay(25)
        }
    }

    // Calculate exact pixel video bounds inside the canvas area based on rotation and aspect ratio
    val normalizedAngle = remember(rotationAngle) { ((rotationAngle % 360f) + 360f) % 360f }
    val isRotated90 = remember(normalizedAngle) { normalizedAngle == 90f || normalizedAngle == 270f }

    val previewGeometry = remember(canvasSize, videoIntrinsicSize, isRotated90, isCropMode, cropRectNormalized) {
        if (canvasSize.width <= 0f || canvasSize.height <= 0f || videoIntrinsicSize.width <= 0f || videoIntrinsicSize.height <= 0f) {
            PreviewGeometry(
                containerWidth = 0f,
                containerHeight = 0f,
                playerWidth = 0f,
                playerHeight = 0f,
                translationX = 0f,
                translationY = 0f,
                videoBounds = Rect.Zero
            )
        } else {
            val cw = canvasSize.width
            val ch = canvasSize.height
            val iw = videoIntrinsicSize.width
            val ih = videoIntrinsicSize.height

            val effectiveW = if (isRotated90) ih else iw
            val effectiveH = if (isRotated90) iw else ih

            if (isCropMode || cropRectNormalized == null) {
                // Full uncropped video fitting canvas
                val scale = kotlin.math.min(cw / effectiveW, ch / effectiveH)
                val dispW = effectiveW * scale
                val dispH = effectiveH * scale

                val left = (cw - dispW) / 2f
                val top = (ch - dispH) / 2f
                val bounds = Rect(left, top, left + dispW, top + dispH)

                val unrotatedW = if (isRotated90) dispH else dispW
                val unrotatedH = if (isRotated90) dispW else dispH

                PreviewGeometry(
                    containerWidth = dispW,
                    containerHeight = dispH,
                    playerWidth = unrotatedW,
                    playerHeight = unrotatedH,
                    translationX = 0f,
                    translationY = 0f,
                    videoBounds = bounds
                )
            } else {
                // Cropped video: display only cropped region enlarged to fit canvas
                val crop = cropRectNormalized!!
                val cropNormW = (crop.right - crop.left).coerceIn(0.01f, 1f)
                val cropNormH = (crop.bottom - crop.top).coerceIn(0.01f, 1f)
                val cropCenterX = (crop.left + crop.right) / 2f
                val cropCenterY = (crop.top + crop.bottom) / 2f

                val croppedUnscaledW = cropNormW * effectiveW
                val croppedUnscaledH = cropNormH * effectiveH

                val scale = kotlin.math.min(cw / croppedUnscaledW, ch / croppedUnscaledH)
                val containerW = croppedUnscaledW * scale
                val containerH = croppedUnscaledH * scale

                val fullVisualW = effectiveW * scale
                val fullVisualH = effectiveH * scale

                val unrotatedW = if (isRotated90) fullVisualH else fullVisualW
                val unrotatedH = if (isRotated90) fullVisualW else fullVisualH

                val transX = (0.5f - cropCenterX) * fullVisualW
                val transY = (0.5f - cropCenterY) * fullVisualH

                PreviewGeometry(
                    containerWidth = containerW,
                    containerHeight = containerH,
                    playerWidth = unrotatedW,
                    playerHeight = unrotatedH,
                    translationX = transX,
                    translationY = transY,
                    videoBounds = Rect.Zero
                )
            }
        }
    }

    // Helper to calculate aspect ratio bounds
    fun applyAspectRatioPreset(ratio: String) {
        selectedAspectRatio = ratio
        if (ratio == "Free") {
            return
        }
        if (ratio == "Full") {
            cropLeft = 0f
            cropTop = 0f
            cropRight = 1f
            cropBottom = 1f
            return
        }
        val targetRatio = when (ratio) {
            "1:1" -> 1f
            "4:3" -> 4f / 3f
            "3:4" -> 3f / 4f
            "16:9" -> 16f / 9f
            "9:16" -> 9f / 16f
            else -> 1f
        }

        val vb = previewGeometry.videoBounds
        if (vb.width > 0f && vb.height > 0f) {
            val videoAspect = vb.width / vb.height
            var normW = 1f
            var normH = 1f

            if (targetRatio > videoAspect) {
                normW = 1f
                normH = (vb.width / targetRatio) / vb.height
            } else {
                normH = 1f
                normW = (vb.height * targetRatio) / vb.width
            }

            normW = normW.coerceIn(0.1f, 1f)
            normH = normH.coerceIn(0.1f, 1f)

            cropLeft = (1f - normW) / 2f
            cropTop = (1f - normH) / 2f
            cropRight = cropLeft + normW
            cropBottom = cropTop + normH
        }
    }

    BackHandler {
        if (isCropMode) {
            cropLeft = cropRectNormalized?.left ?: 0f
            cropTop = cropRectNormalized?.top ?: 0f
            cropRight = cropRectNormalized?.right ?: 1f
            cropBottom = cropRectNormalized?.bottom ?: 1f
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
            // Main Video Canvas Viewport Area
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
                // Clipped Video Container Box
                if (previewGeometry.containerWidth > 0f && previewGeometry.containerHeight > 0f) {
                    Box(
                        modifier = Modifier
                            .width(with(density) { previewGeometry.containerWidth.toDp() })
                            .height(with(density) { previewGeometry.containerHeight.toDp() })
                            .clipToBounds(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Player View Box inside container
                        Box(
                            modifier = Modifier
                                .wrapContentSize(align = Alignment.Center, unbounded = true)
                                .requiredWidth(with(density) { previewGeometry.playerWidth.toDp() })
                                .requiredHeight(with(density) { previewGeometry.playerHeight.toDp() })
                                .graphicsLayer {
                                    rotationZ = animatedRotation
                                    translationX = previewGeometry.translationX
                                    translationY = previewGeometry.translationY
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    val view = LayoutInflater.from(ctx).inflate(R.layout.item_video_editor_player, null, false) as PlayerView
                                    view.player = player
                                    view
                                },
                                update = { view ->
                                    if (view.player != player) {
                                        view.player = player
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                if (!isCropMode) {
                    // Touch Overlay for Video Play/Pause toggle over entire viewport
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { togglePlayPause() })
                            }
                    )

                    // Center Play Button Overlay
                    if (!isPlaying && previewGeometry.containerWidth > 0f) {
                        FilledIconButton(
                            onClick = { togglePlayPause() },
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
                } else {
                    // Full Interactive Video Crop Overlay Canvas aligned directly to videoBounds
                    VideoCropInteractiveCanvas(
                        videoBounds = previewGeometry.videoBounds,
                        cropLeft = cropLeft,
                        cropTop = cropTop,
                        cropRight = cropRight,
                        cropBottom = cropBottom,
                        onCropChanged = { l, t, r, b ->
                            cropLeft = l
                            cropTop = t
                            cropRight = r
                            cropBottom = b
                        },
                        onDragStart = {
                            selectedAspectRatio = "Free"
                        }
                    )
                }
            }

            // Bottom Controls Area
            AnimatedContent(
                targetState = isCropMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "BottomControlsTransition"
            ) { inCropMode ->
                if (!inCropMode) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Action Buttons (Rotate, Crop, Undo, Redo) - Capsule & Circle Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rotate Button (Capsule Shape)
                            Surface(
                                onClick = {
                                    val newAngle = (rotationAngle - 90f) % 360f
                                    pushState(currentState.copy(rotationAngle = newAngle))
                                },
                                shape = CircleShape,
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
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Rotate",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Crop Button (Capsule Shape)
                            Surface(
                                onClick = {
                                    player.pause()
                                    cropLeft = cropRectNormalized?.left ?: 0f
                                    cropTop = cropRectNormalized?.top ?: 0f
                                    cropRight = cropRectNormalized?.right ?: 1f
                                    cropBottom = cropRectNormalized?.bottom ?: 1f
                                    selectedAspectRatio = "Free"
                                    isCropMode = true
                                },
                                shape = CircleShape,
                                color = if (cropRectNormalized != null) Color(0xFF2C3E55) else Color(0xFF1E1E1E),
                                border = if (cropRectNormalized != null) BorderStroke(1.dp, Color(0xFFA8C7FA)) else null,
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
                                        tint = if (cropRectNormalized != null) Color(0xFFA8C7FA) else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (cropRectNormalized != null) "Cropped" else "Crop",
                                        color = if (cropRectNormalized != null) Color(0xFFA8C7FA) else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Undo Button (Circle Shape, Height = 50.dp)
                            Surface(
                                onClick = { undo() },
                                enabled = canUndo,
                                shape = CircleShape,
                                color = Color(0xFF1E1E1E),
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = CustomIcons.Undo,
                                        contentDescription = "Undo",
                                        tint = if (canUndo) Color.White else Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Redo Button (Circle Shape, Height = 50.dp)
                            Surface(
                                onClick = { redo() },
                                enabled = canRedo,
                                shape = CircleShape,
                                color = Color(0xFF1E1E1E),
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = CustomIcons.Redo,
                                        contentDescription = "Redo",
                                        tint = if (canRedo) Color.White else Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Video Trimmer Timeline Strip
                        VideoTimelineTrimmer(
                            thumbnails = timelineThumbnails,
                            totalDurationMs = totalDurationMs,
                            trimStartMs = activeTrimStartMs,
                            trimEndMs = activeTrimEndMs,
                            currentPositionMs = currentPlaybackPositionMs,
                            onTrimChanged = { newStart, newEnd ->
                                player.pause()
                                activeTrimStartMs = newStart
                                activeTrimEndMs = newEnd
                            },
                            onTrimCommit = { newStart, newEnd ->
                                pushState(currentState.copy(trimStartMs = newStart, trimEndMs = newEnd))
                            },
                            onSeek = { seekPos ->
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
                                            startMs = activeTrimStartMs,
                                            endMs = activeTrimEndMs,
                                            rotationAngle = rotationAngle,
                                            cropRectNormalized = cropRectNormalized
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
                } else {
                    // Crop Mode Controls (Aspect Ratio Chips + Cancel / Done)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Aspect Ratio Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val ratios = listOf("Free", "Full", "1:1", "4:3", "3:4", "16:9", "9:16")
                            ratios.forEach { ratio ->
                                val isSelected = selectedAspectRatio == ratio
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        applyAspectRatioPreset(ratio)
                                    },
                                    label = { Text(ratio, fontSize = 13.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color.White,
                                        selectedLabelColor = Color.Black,
                                        containerColor = Color(0xFF1E1E1E),
                                        labelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Crop Actions (Cancel & Done)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    // Cancel crop -> revert to cropRectNormalized
                                    cropLeft = cropRectNormalized?.left ?: 0f
                                    cropTop = cropRectNormalized?.top ?: 0f
                                    cropRight = cropRectNormalized?.right ?: 1f
                                    cropBottom = cropRectNormalized?.bottom ?: 1f
                                    isCropMode = false
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }

                            Button(
                                onClick = {
                                    val isFullFrame = cropLeft <= 0.005f && cropTop <= 0.005f && cropRight >= 0.995f && cropBottom >= 0.995f
                                    val newCropRect = if (isFullFrame) null else RectF(cropLeft, cropTop, cropRight, cropBottom)
                                    pushState(currentState.copy(cropRectNormalized = newCropRect))
                                    isCropMode = false
                                },
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFA8C7FA),
                                    contentColor = Color(0xFF062E6F)
                                ),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = "Done",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
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

private data class PreviewGeometry(
    val containerWidth: Float,
    val containerHeight: Float,
    val playerWidth: Float,
    val playerHeight: Float,
    val translationX: Float,
    val translationY: Float,
    val videoBounds: Rect
)

private data class VideoEditorHistoryState(
    val rotationAngle: Float = 0f,
    val cropRectNormalized: RectF? = null,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L
)

@Composable
private fun VideoCropInteractiveCanvas(
    videoBounds: Rect,
    cropLeft: Float,
    cropTop: Float,
    cropRight: Float,
    cropBottom: Float,
    onCropChanged: (left: Float, top: Float, right: Float, bottom: Float) -> Unit,
    onDragStart: () -> Unit
) {
    val density = LocalDensity.current
    val vb = videoBounds

    if (vb.width <= 0f || vb.height <= 0f) return

    val boxLeft = vb.left + cropLeft * vb.width
    val boxTop = vb.top + cropTop * vb.height
    val boxRight = vb.left + cropRight * vb.width
    val boxBottom = vb.top + cropBottom * vb.height
    val boxWidth = (boxRight - boxLeft).coerceAtLeast(10f)
    val boxHeight = (boxBottom - boxTop).coerceAtLeast(10f)

    var activeDragHandle by remember { mutableStateOf<CropHandle?>(null) }
    val touchRadiusPx = with(density) { 36.dp.toPx() }

    val updatedCropLeft by rememberUpdatedState(cropLeft)
    val updatedCropTop by rememberUpdatedState(cropTop)
    val updatedCropRight by rememberUpdatedState(cropRight)
    val updatedCropBottom by rememberUpdatedState(cropBottom)
    val updatedOnCropChanged by rememberUpdatedState(onCropChanged)
    val updatedOnDragStart by rememberUpdatedState(onDragStart)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(vb) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val x = offset.x
                        val y = offset.y

                        fun isNearPoint(px: Float, py: Float): Boolean {
                            val dx = x - px
                            val dy = y - py
                            return (dx * dx + dy * dy) <= (touchRadiusPx * touchRadiusPx)
                        }

                        // 1. Check 4 corners first
                        activeDragHandle = when {
                            isNearPoint(boxLeft, boxTop) -> CropHandle.TOP_LEFT
                            isNearPoint(boxRight, boxTop) -> CropHandle.TOP_RIGHT
                            isNearPoint(boxLeft, boxBottom) -> CropHandle.BOTTOM_LEFT
                            isNearPoint(boxRight, boxBottom) -> CropHandle.BOTTOM_RIGHT
                            // 2. Check 4 edges
                            kotlin.math.abs(x - boxLeft) <= touchRadiusPx && y in (boxTop - touchRadiusPx)..(boxBottom + touchRadiusPx) -> CropHandle.LEFT
                            kotlin.math.abs(x - boxRight) <= touchRadiusPx && y in (boxTop - touchRadiusPx)..(boxBottom + touchRadiusPx) -> CropHandle.RIGHT
                            kotlin.math.abs(y - boxTop) <= touchRadiusPx && x in (boxLeft - touchRadiusPx)..(boxRight + touchRadiusPx) -> CropHandle.TOP
                            kotlin.math.abs(y - boxBottom) <= touchRadiusPx && x in (boxLeft - touchRadiusPx)..(boxRight + touchRadiusPx) -> CropHandle.BOTTOM
                            // 3. Inside box -> Move
                            x in boxLeft..boxRight && y in boxTop..boxBottom -> CropHandle.MOVE
                            else -> null
                        }

                        if (activeDragHandle != null) {
                            updatedOnDragStart()
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val handle = activeDragHandle ?: return@detectDragGestures
                        val dxNorm = dragAmount.x / vb.width
                        val dyNorm = dragAmount.y / vb.height
                        val minSizeNorm = 0.08f

                        var curL = updatedCropLeft
                        var curT = updatedCropTop
                        var curR = updatedCropRight
                        var curB = updatedCropBottom

                        when (handle) {
                            CropHandle.TOP_LEFT -> {
                                curL = (curL + dxNorm).coerceIn(0f, curR - minSizeNorm)
                                curT = (curT + dyNorm).coerceIn(0f, curB - minSizeNorm)
                            }
                            CropHandle.TOP_RIGHT -> {
                                curR = (curR + dxNorm).coerceIn(curL + minSizeNorm, 1f)
                                curT = (curT + dyNorm).coerceIn(0f, curB - minSizeNorm)
                            }
                            CropHandle.BOTTOM_LEFT -> {
                                curL = (curL + dxNorm).coerceIn(0f, curR - minSizeNorm)
                                curB = (curB + dyNorm).coerceIn(curT + minSizeNorm, 1f)
                            }
                            CropHandle.BOTTOM_RIGHT -> {
                                curR = (curR + dxNorm).coerceIn(curL + minSizeNorm, 1f)
                                curB = (curB + dyNorm).coerceIn(curT + minSizeNorm, 1f)
                            }
                            CropHandle.LEFT -> {
                                curL = (curL + dxNorm).coerceIn(0f, curR - minSizeNorm)
                            }
                            CropHandle.RIGHT -> {
                                curR = (curR + dxNorm).coerceIn(curL + minSizeNorm, 1f)
                            }
                            CropHandle.TOP -> {
                                curT = (curT + dyNorm).coerceIn(0f, curB - minSizeNorm)
                            }
                            CropHandle.BOTTOM -> {
                                curB = (curB + dyNorm).coerceIn(curT + minSizeNorm, 1f)
                            }
                            CropHandle.MOVE -> {
                                val w = curR - curL
                                val h = curB - curT
                                val newLeft = (curL + dxNorm).coerceIn(0f, 1f - w)
                                val newTop = (curT + dyNorm).coerceIn(0f, 1f - h)
                                curL = newLeft
                                curT = newTop
                                curR = newLeft + w
                                curB = newTop + h
                            }
                        }
                        updatedOnCropChanged(curL, curT, curR, curB)
                    },
                    onDragEnd = {
                        activeDragHandle = null
                    },
                    onDragCancel = {
                        activeDragHandle = null
                    }
                )
            }
    ) {
        // 1. Darken background outside the crop box (inside crop box is 100% original color!)
        val dimColor = Color.Black.copy(alpha = 0.65f)
        val cw = size.width
        val ch = size.height

        if (boxTop > 0f) drawRect(dimColor, Offset(0f, 0f), Size(cw, boxTop))
        if (boxBottom < ch) drawRect(dimColor, Offset(0f, boxBottom), Size(cw, ch - boxBottom))
        if (boxLeft > 0f) drawRect(dimColor, Offset(0f, boxTop), Size(boxLeft, boxBottom - boxTop))
        if (boxRight < cw) drawRect(dimColor, Offset(boxRight, boxTop), Size(cw - boxRight, boxBottom - boxTop))

        // 2. White Border of crop window
        drawRect(
            color = Color.White,
            topLeft = Offset(boxLeft, boxTop),
            size = Size(boxWidth, boxHeight),
            style = Stroke(width = 2.dp.toPx())
        )

        // 3. Rule of thirds grid lines
        val oneThirdW = boxWidth / 3f
        val oneThirdH = boxHeight / 3f

        drawLine(Color.White.copy(alpha = 0.45f), Offset(boxLeft + oneThirdW, boxTop), Offset(boxLeft + oneThirdW, boxBottom), strokeWidth = 1.dp.toPx())
        drawLine(Color.White.copy(alpha = 0.45f), Offset(boxLeft + 2 * oneThirdW, boxTop), Offset(boxLeft + 2 * oneThirdW, boxBottom), strokeWidth = 1.dp.toPx())
        drawLine(Color.White.copy(alpha = 0.45f), Offset(boxLeft, boxTop + oneThirdH), Offset(boxRight, boxTop + oneThirdH), strokeWidth = 1.dp.toPx())
        drawLine(Color.White.copy(alpha = 0.45f), Offset(boxLeft, boxTop + 2 * oneThirdH), Offset(boxRight, boxTop + 2 * oneThirdH), strokeWidth = 1.dp.toPx())

        // 4. Corner Handles (Thick L-brackets)
        val cornerLen = 22.dp.toPx()
        val cornerStroke = 4.dp.toPx()

        // Top-Left
        drawLine(Color.White, Offset(boxLeft - 1f, boxTop), Offset(boxLeft + cornerLen, boxTop), cornerStroke)
        drawLine(Color.White, Offset(boxLeft, boxTop - 1f), Offset(boxLeft, boxTop + cornerLen), cornerStroke)

        // Top-Right
        drawLine(Color.White, Offset(boxRight + 1f, boxTop), Offset(boxRight - cornerLen, boxTop), cornerStroke)
        drawLine(Color.White, Offset(boxRight, boxTop - 1f), Offset(boxRight, boxTop + cornerLen), cornerStroke)

        // Bottom-Left
        drawLine(Color.White, Offset(boxLeft - 1f, boxBottom), Offset(boxLeft + cornerLen, boxBottom), cornerStroke)
        drawLine(Color.White, Offset(boxLeft, boxBottom + 1f), Offset(boxLeft, boxBottom - cornerLen), cornerStroke)

        // Bottom-Right
        drawLine(Color.White, Offset(boxRight + 1f, boxBottom), Offset(boxRight - cornerLen, boxBottom), cornerStroke)
        drawLine(Color.White, Offset(boxRight, boxBottom + 1f), Offset(boxRight, boxBottom - cornerLen), cornerStroke)

        // 5. Edge Center Drag Pills
        val pillHalfLen = 10.dp.toPx()
        val pillStroke = 3.5.dp.toPx()

        val midX = (boxLeft + boxRight) / 2f
        val midY = (boxTop + boxBottom) / 2f
        drawLine(Color.White, Offset(midX - pillHalfLen, boxTop), Offset(midX + pillHalfLen, boxTop), pillStroke)
        drawLine(Color.White, Offset(midX - pillHalfLen, boxBottom), Offset(midX + pillHalfLen, boxBottom), pillStroke)
        drawLine(Color.White, Offset(boxLeft, midY - pillHalfLen), Offset(boxLeft, midY + pillHalfLen), pillStroke)
        drawLine(Color.White, Offset(boxRight, midY - pillHalfLen), Offset(boxRight, midY + pillHalfLen), pillStroke)
    }
}

private enum class CropHandle {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    LEFT, RIGHT, TOP, BOTTOM, MOVE
}

@Composable
private fun VideoTimelineTrimmer(
    thumbnails: List<Bitmap>,
    totalDurationMs: Long,
    trimStartMs: Long,
    trimEndMs: Long,
    currentPositionMs: Long,
    onTrimChanged: (startMs: Long, endMs: Long) -> Unit,
    onTrimCommit: (startMs: Long, endMs: Long) -> Unit,
    onSeek: (seekPosMs: Long) -> Unit
) {
    var stripWidthPx by remember { mutableFloatStateOf(0f) }
    val handleWidthDp = 18.dp
    val density = LocalDensity.current
    val handleWidthPx = with(density) { handleWidthDp.toPx() }

    val startFraction = if (totalDurationMs > 0) (trimStartMs.toFloat() / totalDurationMs).coerceIn(0f, 1f) else 0f
    val endFraction = if (totalDurationMs > 0) (trimEndMs.toFloat() / totalDurationMs).coerceIn(0f, 1f) else 1f
    val progressFraction = if (totalDurationMs > 0) (currentPositionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f) else 0f

    val updatedTrimStartMs by rememberUpdatedState(trimStartMs)
    val updatedTrimEndMs by rememberUpdatedState(trimEndMs)
    val updatedTotalDurationMs by rememberUpdatedState(totalDurationMs)
    val updatedOnTrimChanged by rememberUpdatedState(onTrimChanged)
    val updatedOnTrimCommit by rememberUpdatedState(onTrimCommit)
    val updatedOnSeek by rememberUpdatedState(onSeek)

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
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (stripWidthPx > 0 && updatedTotalDurationMs > 0) {
                            val fraction = (offset.x / stripWidthPx).coerceIn(0f, 1f)
                            val seekPos = (fraction * updatedTotalDurationMs).toLong().coerceIn(updatedTrimStartMs, updatedTrimEndMs)
                            updatedOnSeek(seekPos)
                        }
                    }
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
                val usableTrackWidth = (stripWidthPx - 2 * handleWidthPx).coerceAtLeast(1f)
                val leftHandleX = (startFraction * usableTrackWidth).coerceAtLeast(0f)
                val rightHandleX = (endFraction * usableTrackWidth + handleWidthPx).coerceIn(leftHandleX + handleWidthPx, stripWidthPx - handleWidthPx)

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

                // 3. Active Frame Border
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

                // 5. Left Handle (White vertical pill with 3 dots & expanded touch target)
                var leftDragAccumulator by remember { mutableFloatStateOf(0f) }
                var leftTrimStartMsAtDrag by remember { mutableLongStateOf(0L) }

                Box(
                    modifier = Modifier
                        .offset(x = with(density) { (leftHandleX - 10f).coerceAtLeast(0f).toDp() })
                        .width(with(density) { (handleWidthPx + 20f).toDp() })
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    leftDragAccumulator = 0f
                                    leftTrimStartMsAtDrag = updatedTrimStartMs
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    leftDragAccumulator += dragAmount.x
                                    val currentUsableWidth = (stripWidthPx - 2 * handleWidthPx).coerceAtLeast(1f)
                                    val deltaFraction = leftDragAccumulator / currentUsableWidth
                                    val deltaMs = (deltaFraction * updatedTotalDurationMs).toLong()
                                    val minTrimDurationMs = 1000L.coerceAtMost(updatedTotalDurationMs / 4)
                                    val newStartMs = (leftTrimStartMsAtDrag + deltaMs).coerceIn(0L, (updatedTrimEndMs - minTrimDurationMs).coerceAtLeast(0L))

                                    updatedOnTrimChanged(newStartMs, updatedTrimEndMs)
                                    updatedOnSeek(newStartMs)
                                },
                                onDragEnd = {
                                    updatedOnTrimCommit(updatedTrimStartMs, updatedTrimEndMs)
                                },
                                onDragCancel = {
                                    updatedOnTrimCommit(updatedTrimStartMs, updatedTrimEndMs)
                                }
                            )
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = with(density) { (leftHandleX - (leftHandleX - 10f).coerceAtLeast(0f)).toDp() })
                            .width(handleWidthDp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                            .background(Color.White),
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

                // 6. Right Handle (White vertical pill with 3 dots & expanded touch target)
                var rightDragAccumulator by remember { mutableFloatStateOf(0f) }
                var rightTrimEndMsAtDrag by remember { mutableLongStateOf(0L) }

                Box(
                    modifier = Modifier
                        .offset(x = with(density) { (rightHandleX - 10f).coerceAtLeast(0f).toDp() })
                        .width(with(density) { (handleWidthPx + 20f).toDp() })
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    rightDragAccumulator = 0f
                                    rightTrimEndMsAtDrag = updatedTrimEndMs
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    rightDragAccumulator += dragAmount.x
                                    val currentUsableWidth = (stripWidthPx - 2 * handleWidthPx).coerceAtLeast(1f)
                                    val deltaFraction = rightDragAccumulator / currentUsableWidth
                                    val deltaMs = (deltaFraction * updatedTotalDurationMs).toLong()
                                    val minTrimDurationMs = 1000L.coerceAtMost(updatedTotalDurationMs / 4)
                                    val newEndMs = (rightTrimEndMsAtDrag + deltaMs).coerceIn(updatedTrimStartMs + minTrimDurationMs, updatedTotalDurationMs)

                                    updatedOnTrimChanged(updatedTrimStartMs, newEndMs)
                                    updatedOnSeek(newEndMs)
                                },
                                onDragEnd = {
                                    updatedOnTrimCommit(updatedTrimStartMs, updatedTrimEndMs)
                                },
                                onDragCancel = {
                                    updatedOnTrimCommit(updatedTrimStartMs, updatedTrimEndMs)
                                }
                            )
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = with(density) { (rightHandleX - (rightHandleX - 10f).coerceAtLeast(0f)).toDp() })
                            .width(handleWidthDp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                            .background(Color.White),
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
                    // Keep Editing button (Capsule Shape)
                    Surface(
                        onClick = onKeepEditing,
                        shape = CircleShape,
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

                    // Discard button (Capsule Shape)
                    Surface(
                        onClick = onDiscard,
                        shape = CircleShape,
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
