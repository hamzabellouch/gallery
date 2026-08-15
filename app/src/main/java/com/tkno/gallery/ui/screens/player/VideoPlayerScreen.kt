package com.tkno.gallery.ui.screens.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.PixelFormat
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
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.tkno.gallery.R
import com.tkno.gallery.theme.TaskbarActivePrimaryDark
import com.tkno.gallery.ui.components.CustomIcons
import com.tkno.gallery.util.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.media3.ui.AspectRatioFrameLayout

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun enterPictureInPictureMode(context: Context, videoWidth: Int = 0, videoHeight: Int = 0) {
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

private suspend fun captureAndSaveVideoFrame(
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


@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoUri: Uri,
    videoTitle: String,
    onBackClick: () -> Unit,
    showControls: Boolean = true,
    onTap: () -> Unit = {},
    onSwipeUp: () -> Unit = {},
    onNextClick: (() -> Unit)? = null,
    onPreviousClick: (() -> Unit)? = null
) {
    // Intercept system back button & system back swipe gesture reliably
    BackHandler {
        onBackClick()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCapturingFrame by remember { mutableStateOf(false) }

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
            }
        } else {
            onDispose {}
        }
    }


    var isPlaying by remember { mutableStateOf(true) }
    var isLooping by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }
    var videoFps by remember { mutableFloatStateOf(0f) }
    var isFirstFrameRendered by remember { mutableStateOf(false) }
    val primaryAccent = MaterialTheme.colorScheme.primary

    // MAXIMUM PERFORMANCE STABLE ENGINE: Hardware Accelerated Decoders & Adaptive Dynamic Buffers
    val exoPlayer: ExoPlayer = remember(videoUri) {
        com.tkno.gallery.util.VideoEngineManager.init(context)
        val renderersFactory = com.tkno.gallery.util.VideoEngineManager.renderersFactory

        // Adaptive Dynamic Buffers matching Device Memory Profile
        val memoryProfile = com.tkno.gallery.util.MemoryManager.getMemoryProfile(context)
        val (minBufferMs, maxBufferMs) = when (memoryProfile.maxAppMemoryLimitMb) {
            200 -> Pair(5000, 15000)
            500 -> Pair(10000, 25000)
            800 -> Pair(12000, 35000)
            else -> Pair(15000, 45000)
        }

        // Zero-Wait LoadControl: Instant 0ms playback start without buffering delays
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ minBufferMs,
                /* maxBufferMs = */ maxBufferMs,
                /* bufferForPlaybackMs = */ 0,
                /* bufferForPlaybackAfterRebufferMs = */ 100
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val mediaSourceFactory = DefaultMediaSourceFactory(context.applicationContext)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        ExoPlayer.Builder(context.applicationContext, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
            .build().apply {
                val mediaItem = MediaItem.fromUri(videoUri)
                setMediaItem(mediaItem)
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
                playWhenReady = true
            }
    }

    LaunchedEffect(isLooping) {
        exoPlayer.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    playbackError = null
                }
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
                playbackError = error.localizedMessage ?: "Playback Error (${error.errorCodeName})"
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Progress ticker: Only update when controls are visible to avoid unnecessary UI thread recomposition overhead
    LaunchedEffect(isPlaying, showControls) {
        while (isPlaying) {
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            delay(if (showControls) 200 else 1000)
        }
    }

    val resBadgeResId = remember(videoWidth, videoHeight, videoTitle) {
        val maxDim = maxOf(videoWidth, videoHeight)
        val minDim = minOf(videoWidth, videoHeight)

        fun hasTag(title: String, tag: String): Boolean {
            val regex = Regex("(?i)(^|[^a-z0-9])${Regex.escape(tag)}($|[^a-z0-9])")
            return regex.containsMatchIn(title)
        }

        when {
            maxDim >= 7000 || minDim >= 3800 || hasTag(videoTitle, "8K") -> R.drawable.ic_8k
            minDim >= 2160 || maxDim >= 3500 || hasTag(videoTitle, "4K") || hasTag(videoTitle, "UHD") || hasTag(videoTitle, "2160p") -> R.drawable.ic_4k
            minDim >= 1400 || (maxDim >= 1440 && minDim >= 1080 && maxDim < 1920) || hasTag(videoTitle, "2K") || hasTag(videoTitle, "QHD") || hasTag(videoTitle, "1440p") -> R.drawable.ic_2k
            maxDim >= 1700 || minDim >= 950 || hasTag(videoTitle, "1080p") || hasTag(videoTitle, "FHD") || hasTag(videoTitle, "1080") -> R.drawable.ic_fhd
            maxDim >= 1150 || minDim >= 650 || hasTag(videoTitle, "720p") || hasTag(videoTitle, "HD") || hasTag(videoTitle, "720") -> R.drawable.ic_hd
            else -> 0
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        PlayerGestureController(
            onSingleTap = {
                onTap()
            },
            onDoubleTapLeft = {
                val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                exoPlayer.seekTo(newPos)
            },
            onDoubleTapRight = {
                val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                exoPlayer.seekTo(newPos)
            },
            onVolumeChange = {},
            onBrightnessChange = {},
            onSwipeUp = onSwipeUp
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        keepScreenOn = true
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setOnTouchListener { _, _ -> false }

                        // Enforce Hardware Composer SurfaceView Overlay & Auto Refresh Rate Matching for Android 12/13 (Zero Judder)
                        val videoSurface = videoSurfaceView
                        if (videoSurface is SurfaceView) {
                            videoSurface.holder.setKeepScreenOn(true)
                            videoSurface.setZOrderOnTop(false)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                try {
                                    val surface = videoSurface.holder.surface
                                    val setFrameRateMethod = surface?.javaClass?.getMethod(
                                        "setFrameRate",
                                        Float::class.javaPrimitiveType,
                                        Int::class.javaPrimitiveType
                                    )
                                    val targetFps = if (videoFps > 0f) videoFps else 0f
                                    setFrameRateMethod?.invoke(surface, targetFps, Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE)
                                } catch (_: Throwable) {}
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Mask initial surface & decoder initialization until the first frame is actually rendered
        if (!isFirstFrameRendered) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }

        // Buffering Indicator
        if (isBuffering && playbackError == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = primaryAccent
            )
        }

        // Error Retry Overlay
        playbackError?.let { err ->
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Unable to play video",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        playbackError = null
                        exoPlayer.prepare()
                        exoPlayer.play()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryAccent)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry")
                }
            }
        }

        // Overlay Controls
        AnimatedVisibility(
            visible = showControls && !isInPipMode && playbackError == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Center Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (onPreviousClick != null) {
                        IconButton(onClick = { onPreviousClick() }) {
                            Icon(
                                imageVector = CustomIcons.SkipPrevious,
                                contentDescription = "Previous Video",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    IconButton(onClick = {
                        exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0))
                    }) {
                        Icon(
                            imageVector = CustomIcons.Replay10,
                            contentDescription = "-10s",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                // If video ended, seek back to start before playing
                                if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                    exoPlayer.seekTo(0)
                                }
                                exoPlayer.play()
                            }
                        },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) CustomIcons.Pause else CustomIcons.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(52.dp)
                        )
                    }

                    IconButton(onClick = {
                        exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration))
                    }) {
                        Icon(
                            imageVector = CustomIcons.Forward10,
                            contentDescription = "+10s",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    if (onNextClick != null) {
                        IconButton(onClick = { onNextClick() }) {
                            Icon(
                                imageVector = CustomIcons.SkipNext,
                                contentDescription = "Next Video",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // Bottom Progress Bar & Time Display
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${FormatUtils.formatDuration(currentPositionMs)} / ${FormatUtils.formatDuration(durationMs)}",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    playbackSpeed = when (playbackSpeed) {
                                        1.0f -> 1.25f
                                        1.25f -> 1.5f
                                        1.5f -> 2.0f
                                        2.0f -> 0.5f
                                        else -> 1.0f
                                    }
                                    exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${playbackSpeed}x",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            // Loop Video Toggle Button
                            IconButton(
                                onClick = {
                                    isLooping = !isLooping
                                }
                            ) {
                                Icon(
                                    imageVector = CustomIcons.Repeat,
                                    contentDescription = "Loop Video",
                                    tint = if (isLooping) primaryAccent else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                IconButton(
                                    onClick = {
                                        enterPictureInPictureMode(context, videoWidth, videoHeight)
                                    }
                                ) {
                                    Icon(
                                        imageVector = CustomIcons.PictureInPicture,
                                        contentDescription = "Picture in Picture",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Capture Video Frame Screenshot Button
                            IconButton(
                                onClick = {
                                    if (!isCapturingFrame) {
                                        isCapturingFrame = true
                                        coroutineScope.launch {
                                            val success = captureAndSaveVideoFrame(context, videoUri, currentPositionMs)
                                            isCapturingFrame = false
                                            if (success) {
                                                Toast.makeText(context, "Frame saved to gallery successfully", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Could not capture frame", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                enabled = !isCapturingFrame
                            ) {
                                Icon(
                                    imageVector = CustomIcons.FitScreen,
                                    contentDescription = "Capture Frame",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }


                    Slider(
                        value = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f,
                        onValueChange = { fraction ->
                            val newPosition = (fraction * durationMs).toLong()
                            exoPlayer.seekTo(newPosition)
                            currentPositionMs = newPosition
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = primaryAccent,
                            activeTrackColor = primaryAccent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    }
}
