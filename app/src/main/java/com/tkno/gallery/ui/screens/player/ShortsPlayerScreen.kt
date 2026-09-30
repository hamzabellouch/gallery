package com.tkno.gallery.ui.screens.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Rational
import android.view.Surface
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.tkno.gallery.data.model.MediaItem
import com.tkno.gallery.ui.components.CustomIcons
import com.tkno.gallery.util.FormatUtils
import com.tkno.gallery.util.VideoEngineManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun enterPip(context: Context, videoWidth: Int = 0, videoHeight: Int = 0) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val activity = context.findActivity() ?: return
        val builder = PictureInPictureParams.Builder()
        if (videoWidth > 0 && videoHeight > 0) {
            val aspectRatio = Rational(videoWidth, videoHeight)
            val floatRatio = aspectRatio.toFloat()
            if (floatRatio in 0.418f..2.39f) {
                builder.setAspectRatio(aspectRatio)
            }
        }
        try {
            activity.enterPictureInPictureMode(builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

private suspend fun captureAndSaveFrame(
    context: Context,
    videoUri: Uri,
    currentPositionMs: Long
): Boolean = withContext(Dispatchers.IO) {
    var retriever: MediaMetadataRetriever? = null
    try {
        retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)
        val timeUs = currentPositionMs.coerceAtLeast(0L) * 1000L

        var bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
        if (bitmap == null) {
            bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }
        if (bitmap == null) return@withContext false

        val filename = "FRAME_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Gallery")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return@withContext false

        resolver.openOutputStream(imageUri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(imageUri, contentValues, null, null)
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    } finally {
        try {
            retriever?.release()
        } catch (_: Exception) {}
    }
}

@Composable
fun ShortsPlayerScreen(
    mediaItems: List<MediaItem>,
    initialIndex: Int,
    onBackClick: () -> Unit,
    onCurrentItemChanged: ((MediaItem) -> Unit)? = null,
    onToggleFavorite: ((MediaItem) -> Unit)? = null,
    onDeleteMediaItem: ((MediaItem) -> Unit)? = null,
    onEditClick: ((MediaItem) -> Unit)? = null
) {
    if (mediaItems.isEmpty()) {
        LaunchedEffect(Unit) { onBackClick() }
        return
    }

    BackHandler {
        onBackClick()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = remember(context) { context.findActivity() as? ComponentActivity }

    var isInPipMode by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                activity?.isInPictureInPictureMode == true
            } else false
        )
    }

    DisposableEffect(activity) {
        if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val listener = Consumer<PictureInPictureModeChangedInfo> { info ->
                isInPipMode = info.isInPictureInPictureMode
            }
            activity.addOnPictureInPictureModeChangedListener(listener)
            onDispose {
                activity.removeOnPictureInPictureModeChangedListener(listener)
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        } else {
            onDispose {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    val startIndex = initialIndex.coerceIn(0, mediaItems.lastIndex)
    val pagerState = rememberPagerState(
        initialPage = startIndex,
        pageCount = { mediaItems.size }
    )

    LaunchedEffect(pagerState.currentPage, mediaItems) {
        val current = mediaItems.getOrNull(pagerState.currentPage)
        if (current != null) {
            onCurrentItemChanged?.invoke(current)
        }
    }

    var pendingDeleteItem by remember { mutableStateOf<MediaItem?>(null) }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingDeleteItem?.let { item ->
                onDeleteMediaItem?.invoke(item)
            }
        }
        pendingDeleteItem = null
    }

    fun deleteItem(item: MediaItem) {
        pendingDeleteItem = item
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intentSender = MediaStore.createTrashRequest(
                    context.contentResolver,
                    listOf(item.uri),
                    true
                ).intentSender
                deleteLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            } catch (e: Exception) {
                e.printStackTrace()
                pendingDeleteItem = null
            }
        } else {
            try {
                val rows = context.contentResolver.delete(item.uri, null, null)
                if (rows > 0) {
                    onDeleteMediaItem?.invoke(item)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingDeleteItem = null
            }
        }
    }

    fun shareItem(item: MediaItem) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (item.mimeType.isNotBlank()) item.mimeType else "video/*"
            putExtra(Intent.EXTRA_STREAM, item.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        VerticalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize(),
            key = { index -> mediaItems.getOrNull(index)?.id ?: index }
        ) { page ->
            val item = mediaItems.getOrNull(page)
            if (item != null) {
                val isCurrentPage = (pagerState.currentPage == page)
                ShortsVideoPageItem(
                    item = item,
                    isCurrentPage = isCurrentPage,
                    isInPipMode = isInPipMode,
                    onToggleFavorite = { onToggleFavorite?.invoke(item) },
                    onEditClick = { onEditClick?.invoke(item) },
                    onDeleteClick = { deleteItem(item) },
                    onShareClick = { shareItem(item) }
                )
            }
        }

        // Top-Left Floating Back Button
        if (!isInPipMode) {
            Surface(
                shape = CircleShape,
                color = if (isDark) Color.Black.copy(alpha = 0.45f) else MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                contentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp,
                shadowElevation = if (isDark) 0.dp else 6.dp,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 8.dp)
                    .size(44.dp)
                    .align(Alignment.TopStart)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = CustomIcons.ChevronLeft,
                        contentDescription = "Back",
                        tint = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShortsVideoPageItem(
    item: MediaItem,
    isCurrentPage: Boolean,
    isInPipMode: Boolean,
    onToggleFavorite: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val primaryAccent = MaterialTheme.colorScheme.primary

    val haptic = LocalHapticFeedback.current

    var isPlaying by remember { mutableStateOf(true) }
    var isLooping by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }
    var videoFps by remember { mutableFloatStateOf(0f) }
    var isFirstFrameRendered by remember { mutableStateOf(false) }
    var isCapturingFrame by remember { mutableStateOf(false) }
    var showPlayPauseIndicator by remember { mutableStateOf(false) }
    var showMorePopup by remember { mutableStateOf(false) }

    // Double-tap heart pop animation states
    val heartScale = remember { Animatable(0f) }
    val heartAlpha = remember { Animatable(0f) }
    val heartRotation = remember { Animatable(0f) }
    var showDoubleTapHeart by remember { mutableStateOf(false) }

    fun triggerDoubleTapLike() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (!item.isFavorite) {
            onToggleFavorite()
        }
        coroutineScope.launch {
            showDoubleTapHeart = true
            heartRotation.snapTo((-15..15).random().toFloat())
            heartScale.snapTo(0.2f)
            heartAlpha.snapTo(1f)
            // Pop in with bouncy spring
            heartScale.animateTo(
                targetValue = 1.35f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            heartScale.animateTo(
                targetValue = 1.15f,
                animationSpec = tween(100)
            )
            delay(280)
            // Float upwards and fade out
            launch {
                heartScale.animateTo(1.55f, tween(250))
            }
            heartAlpha.animateTo(0f, tween(250))
            showDoubleTapHeart = false
        }
    }

    val exoPlayer: ExoPlayer? = remember(item.uri, isCurrentPage) {
        if (isCurrentPage) {
            VideoEngineManager.createHardenedPlayer(context).apply {
                val mediaItem = Media3Item.fromUri(item.uri)
                setMediaItem(mediaItem)
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
                playWhenReady = true
            }
        } else {
            null
        }
    }

    LaunchedEffect(isLooping, exoPlayer) {
        exoPlayer?.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    DisposableEffect(exoPlayer) {
        if (exoPlayer == null) return@DisposableEffect onDispose {}

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }

            override fun onRenderedFirstFrame() {
                isFirstFrameRendered = true
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                videoWidth = videoSize.width
                videoHeight = videoSize.height
            }

            override fun onTracksChanged(tracks: Tracks) {
                for (group in tracks.groups) {
                    if (group.type == C.TRACK_TYPE_VIDEO) {
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            if (format.frameRate > 0f) {
                                videoFps = format.frameRate
                            }
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                error.printStackTrace()
                try {
                    exoPlayer.prepare()
                    exoPlayer.play()
                } catch (_: Exception) {}
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPlaying, isCurrentPage) {
        if (isCurrentPage && exoPlayer != null) {
            while (isPlaying) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
                delay(100L)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color.Black else MaterialTheme.colorScheme.background)
            .pointerInput(exoPlayer, item.isFavorite) {
                detectTapGestures(
                    onDoubleTap = {
                        triggerDoubleTapLike()
                    },
                    onTap = {
                        exoPlayer?.let { player ->
                            if (player.isPlaying) {
                                player.pause()
                            } else {
                                if (player.playbackState == Player.STATE_ENDED) {
                                    player.seekTo(0)
                                }
                                player.play()
                            }
                            showPlayPauseIndicator = true
                            coroutineScope.launch {
                                delay(700)
                                showPlayPauseIndicator = false
                            }
                        }
                    }
                )
            }
    ) {
        if (isCurrentPage && exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        keepScreenOn = true
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        setShutterBackgroundColor(if (isDark) android.graphics.Color.BLACK else android.graphics.Color.TRANSPARENT)
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setOnTouchListener { _, _ -> false }

                        val videoSurface = videoSurfaceView
                        if (videoSurface is SurfaceView) {
                            videoSurface.holder.setKeepScreenOn(true)
                            videoSurface.setZOrderOnTop(false)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Mask initial surface
        if (!isFirstFrameRendered && isCurrentPage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDark) Color.Black else MaterialTheme.colorScheme.background)
            )
        }

        // Buffering Indicator
        if (isBuffering && isCurrentPage) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = primaryAccent
            )
        }

        // Animated Play/Pause indicator in center
        AnimatedVisibility(
            visible = (!isPlaying || showPlayPauseIndicator) && !isBuffering && isCurrentPage,
            enter = fadeIn(tween(150)) + scaleIn(tween(150)),
            exit = fadeOut(tween(250)) + scaleOut(tween(250)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isDark) Color.Black.copy(alpha = 0.5f) else MaterialTheme.colorScheme.background.copy(alpha = 0.75f),
                shadowElevation = if (isDark) 0.dp else 4.dp,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) CustomIcons.Pause else CustomIcons.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }

        // Double-Tap Animated Big Heart in Center of Screen (TikTok Style)
        if (showDoubleTapHeart && isCurrentPage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Liked",
                    tint = Color(0xFFFF2B54),
                    modifier = Modifier
                        .size(116.dp)
                        .graphicsLayer {
                            scaleX = heartScale.value
                            scaleY = heartScale.value
                            alpha = heartAlpha.value
                            rotationZ = heartRotation.value
                        }
                )
            }
        }

        // Subtle gradient overlay at bottom for readability
        if (!isInPipMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                if (isDark) Color.Black.copy(alpha = 0.75f) else MaterialTheme.colorScheme.background.copy(alpha = 0.75f)
                            )
                        )
                    )
            )
        }

        // Right-Side TikTok Action Column
        if (!isInPipMode) {
            val actionButtonTint = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 12.dp, bottom = 56.dp)
            ) {
                // 1. Favorite (Heart)
                TikTokActionButton(
                    icon = if (item.isFavorite) Icons.Filled.Favorite else CustomIcons.Favorite,
                    label = "Favorite",
                    tint = if (item.isFavorite) Color(0xFFFF2B54) else actionButtonTint,
                    onClick = onToggleFavorite
                )

                // 2. Edit
                TikTokActionButton(
                    icon = CustomIcons.Edit,
                    label = "Edit",
                    tint = actionButtonTint,
                    onClick = onEditClick
                )

                // 3. Trash / Delete
                TikTokActionButton(
                    icon = CustomIcons.Delete,
                    label = "Trash",
                    tint = actionButtonTint,
                    onClick = onDeleteClick
                )

                // 4. Share
                TikTokActionButton(
                    icon = CustomIcons.Share,
                    label = "Share",
                    tint = actionButtonTint,
                    onClick = onShareClick
                )

                // 5. More (Three horizontal dots)
                TikTokActionButton(
                    icon = CustomIcons.MoreHoriz,
                    label = "More",
                    tint = actionButtonTint,
                    onClick = { showMorePopup = true }
                )
            }
        }

        // Bottom Controls: Time & Progress Bar (clean & lowered to bottom edge)
        if (!isInPipMode) {
            val bottomTimeColor = if (isDark) Color.White else MaterialTheme.colorScheme.onBackground
            val inactiveTrackColor = (if (isDark) Color.White else MaterialTheme.colorScheme.onBackground).copy(alpha = 0.35f)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 14.dp, end = 14.dp, bottom = 6.dp)
            ) {
                // Time Display
                Text(
                    text = "${FormatUtils.formatDuration(currentPositionMs)} / ${FormatUtils.formatDuration(durationMs)}",
                    color = bottomTimeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )

                // Slider Progress Bar (lowered close to the bottom edge)
                Slider(
                    value = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f,
                    onValueChange = { fraction ->
                        val newPosition = (fraction * durationMs).toLong()
                        exoPlayer?.seekTo(newPosition)
                        currentPositionMs = newPosition
                    },
                    thumb = {
                        SliderDefaults.Thumb(
                            interactionSource = remember { MutableInteractionSource() },
                            thumbSize = DpSize(12.dp, 12.dp),
                            colors = SliderDefaults.colors(thumbColor = primaryAccent)
                        )
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = primaryAccent,
                        activeTrackColor = primaryAccent,
                        inactiveTrackColor = inactiveTrackColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                )
            }
        }

        // "More" Floating Card Popup Dialog (Styled after CustomizerBottomSheetDialog)
        if (showMorePopup && !isInPipMode) {
            Dialog(
                onDismissRequest = { showMorePopup = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showMorePopup = false }
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        color = if (isDark) Color.Black.copy(alpha = 0.5f) else MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                        shape = RoundedCornerShape(28.dp),
                        shadowElevation = 8.dp,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 22.dp, end = 22.dp, bottom = 24.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { /* prevent dismissing when clicking card */ }
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 18.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val moreItemTint = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface

                            // 1. Playback Speed
                            MorePopupItem(
                                icon = null,
                                customBadgeText = "${playbackSpeed}x",
                                label = "Speed",
                                onClick = {
                                    playbackSpeed = when (playbackSpeed) {
                                        1.0f -> 1.25f
                                        1.25f -> 1.5f
                                        1.5f -> 2.0f
                                        2.0f -> 0.5f
                                        else -> 1.0f
                                    }
                                    exoPlayer?.playbackParameters = PlaybackParameters(playbackSpeed)
                                }
                            )

                            // 2. Loop / Replay
                            MorePopupItem(
                                icon = CustomIcons.Repeat,
                                tint = if (isLooping) primaryAccent else moreItemTint,
                                label = "Loop",
                                onClick = { isLooping = !isLooping }
                            )

                            // 3. Picture in Picture
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                MorePopupItem(
                                    icon = CustomIcons.PictureInPicture,
                                    label = "PiP",
                                    onClick = {
                                        showMorePopup = false
                                        enterPip(context, videoWidth, videoHeight)
                                    }
                                )
                            }

                            // 4. Frame Capture / Screenshot
                            MorePopupItem(
                                icon = CustomIcons.ScreenshotFrame2,
                                label = "Screenshot",
                                onClick = {
                                    if (!isCapturingFrame) {
                                        isCapturingFrame = true
                                        coroutineScope.launch {
                                            val success = captureAndSaveFrame(context, item.uri, currentPositionMs)
                                            isCapturingFrame = false
                                            if (success) {
                                                Toast.makeText(context, "Frame saved to gallery successfully", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Could not capture frame", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            )

                            // 5. Rotate Screen Orientation
                            val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                            MorePopupItem(
                                icon = CustomIcons.MobileRotate,
                                tint = if (isLandscape) primaryAccent else moreItemTint,
                                label = "Rotate",
                                onClick = {
                                    val act = context.findActivity() ?: return@MorePopupItem
                                    if (isLandscape) {
                                        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    } else {
                                        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MorePopupItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    customBadgeText: String? = null,
    label: String,
    tint: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val effectiveTint = if (tint == Color.Unspecified) {
        if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    } else tint

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = effectiveTint,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (customBadgeText != null) {
                    Text(
                        text = customBadgeText,
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = if (isDark) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun TikTokActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (isDark) Color.Black.copy(alpha = 0.45f) else MaterialTheme.colorScheme.background.copy(alpha = 0.88f),
            contentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
            shadowElevation = if (isDark) 0.dp else 4.dp,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = if (isDark) Color.White else MaterialTheme.colorScheme.onBackground,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
