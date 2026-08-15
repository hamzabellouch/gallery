package com.tkno.gallery.ui.screens.viewer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import com.tkno.gallery.data.model.MediaItem
import com.tkno.gallery.ui.components.CustomIcons
import com.tkno.gallery.ui.screens.player.VideoPlayerScreen
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.tkno.gallery.theme.TaskbarActivePrimaryDark
import com.tkno.gallery.util.ExifData
import com.tkno.gallery.util.ExifUtils
import com.tkno.gallery.util.FormatUtils
import com.tkno.gallery.util.MediaMetadataUtils
import kotlinx.coroutines.launch

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MediaPagerScreen(
    mediaItems: List<MediaItem>,
    initialIndex: Int,
    onBackClick: () -> Unit,
    onToggleFavorite: ((MediaItem) -> Unit)? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    if (mediaItems.isEmpty()) {
        LaunchedEffect(Unit) { onBackClick() }
        return
    }

    val startIndex = initialIndex.coerceIn(0, mediaItems.lastIndex)
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { mediaItems.size })
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() as? ComponentActivity }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

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

    val coroutineScope = rememberCoroutineScope()
    var showBars by remember { mutableStateOf(true) }
    var showExifSheet by remember { mutableStateOf(false) }
    var exifData by remember { mutableStateOf<ExifData?>(null) }
    var isZoomedIn by remember { mutableStateOf(false) }
    var resetZoomTrigger by remember { mutableIntStateOf(0) }



    val currentItem = mediaItems.getOrNull(pagerState.currentPage) ?: mediaItems[startIndex]

    LaunchedEffect(currentItem.uri) {
        if (!currentItem.isVideo) {
            exifData = ExifUtils.readExif(context, currentItem.uri)
        } else {
            exifData = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isZoomedIn,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 16.dp,
            key = { index -> mediaItems[index].id }
        ) { page ->
            val item = mediaItems[page]
            val isCurrentPage = (pagerState.currentPage == page)

            var pageModifier = Modifier.fillMaxSize()
            if (isCurrentPage && sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    pageModifier = pageModifier.sharedElement(
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

            Box(modifier = pageModifier) {
                if (item.isVideo) {
                    if (isCurrentPage) {
                        VideoPlayerScreen(
                            videoUri = item.uri,
                            videoTitle = item.name,
                            onBackClick = onBackClick,
                            showControls = showBars,
                            onTap = { showBars = !showBars },
                            onSwipeUp = { showExifSheet = true },
                            onNextClick = if (page < mediaItems.size - 1) {
                                {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(page + 1)
                                    }
                                }
                            } else null,
                            onPreviousClick = if (page > 0) {
                                {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(page - 1)
                                    }
                                }
                            } else null
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        )
                    }
                } else {
                    TilingImageView(
                        imageUri = item.uri,
                        modifier = Modifier.fillMaxSize(),
                        resetZoomTrigger = resetZoomTrigger,
                        onTap = { showBars = !showBars },
                        onSwipeUp = { showExifSheet = true },
                        onScaleChanged = { scale ->
                            if (isCurrentPage) {
                                isZoomedIn = (scale > 1.05f)
                            }
                        }
                    )
                }
            }
        }

        // Floating FitScreen Button for Zoomed Images (Only visible when image is zoomed in)
        AnimatedVisibility(
            visible = !currentItem.isVideo && isZoomedIn && !isInPipMode,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 16.dp)
        ) {
            FilledTonalIconButton(
                onClick = {
                    resetZoomTrigger++
                    isZoomedIn = false
                },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.65f),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = CustomIcons.FitScreen,
                    contentDescription = "Fit to Screen",
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Top Action Bar
        AnimatedVisibility(
            visible = showBars && !isInPipMode,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = CustomIcons.ChevronLeft,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val act = context.findActivity() ?: return@IconButton
                            if (isLandscape) {
                                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            } else {
                                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            }
                        }
                    ) {
                        Icon(
                            imageVector = CustomIcons.MobileRotate,
                            contentDescription = "Rotate Screen",
                            tint = if (isLandscape) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
            )
        }

        // Bottom Action Bar
        AnimatedVisibility(
            visible = showBars && !isInPipMode,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(72.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clickable {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = currentItem.mimeType
                                    putExtra(Intent.EXTRA_STREAM, currentItem.uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                            }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = CustomIcons.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Share",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }

                    // Auto (Images only)
                    if (!currentItem.isVideo) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clickable { }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = CustomIcons.WandStars,
                                contentDescription = "Auto",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Auto",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Favorite
                    val isFav = currentItem.isFavorite
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clickable { onToggleFavorite?.invoke(currentItem) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Filled.Favorite else CustomIcons.Favorite,
                            contentDescription = if (isFav) "Unfavorite" else "Favorite",
                            tint = if (isFav) Color.Red else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isFav) "Unfavorite" else "Favorite",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }

                    // Edit
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clickable { }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = CustomIcons.Edit,
                            contentDescription = "Edit",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Edit",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }

                    // Trash
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clickable { }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = CustomIcons.Delete,
                            contentDescription = "Trash",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Trash",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Details BottomSheet
        if (showExifSheet) {
            val meta = remember(currentItem.uri) {
                MediaMetadataUtils.extractMetadata(context, currentItem)
            }

            ModalBottomSheet(
                onDismissRequest = { showExifSheet = false },
                containerColor = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // General Information
                    DetailRow(label = "Filename", value = meta.filename)
                    DetailRow(
                        label = "Resolution",
                        value = meta.resolution + if (meta.badgeLabel.isNotEmpty()) " (${meta.badgeLabel})" else ""
                    )
                    if (meta.fps.isNotEmpty()) {
                        DetailRow(label = "Frame Rate (FPS)", value = meta.fps)
                    }
                    if (meta.megapixels.isNotEmpty()) {
                        DetailRow(label = "Megapixels", value = meta.megapixels)
                    }
                    if (meta.aspectRatio.isNotEmpty()) {
                        DetailRow(label = "Aspect Ratio", value = meta.aspectRatio)
                    }
                    DetailRow(label = "File Size", value = meta.fileSize)
                    DetailRow(label = "Date & Time", value = meta.dateAdded)
                    DetailRow(label = "Format / MIME", value = meta.mimeType)

                    // Directory & Storage Location
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    if (meta.parentFolder.isNotEmpty()) {
                        DetailRow(label = "Folder", value = meta.parentFolder)
                    }
                    if (meta.fullDirectory.isNotEmpty()) {
                        DetailRow(label = "Full Directory", value = meta.fullDirectory)
                    }

                    // Media Specific Details
                    if (meta.isVideo) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        if (meta.duration.isNotEmpty()) DetailRow(label = "Duration", value = meta.duration)
                        if (meta.bitrate.isNotEmpty()) DetailRow(label = "Bitrate", value = meta.bitrate)
                        if (meta.videoCodec.isNotEmpty()) DetailRow(label = "Video Codec", value = meta.videoCodec)
                        if (meta.audioCodec.isNotEmpty()) DetailRow(label = "Audio Codec", value = meta.audioCodec)
                        if (meta.rotation.isNotEmpty()) DetailRow(label = "Rotation", value = meta.rotation)
                    } else {
                        // Image / EXIF Specific Details
                        if (meta.cameraModel.isNotEmpty() || meta.aperture.isNotEmpty() || meta.iso.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            if (meta.cameraModel.isNotEmpty()) DetailRow(label = "Camera", value = "${meta.cameraMake} ${meta.cameraModel}".trim())
                            if (meta.aperture.isNotEmpty()) DetailRow(label = "Aperture", value = meta.aperture)
                            if (meta.exposureTime.isNotEmpty()) DetailRow(label = "Shutter Speed", value = meta.exposureTime)
                            if (meta.iso.isNotEmpty()) DetailRow(label = "ISO Speed", value = meta.iso)
                            if (meta.focalLength.isNotEmpty()) DetailRow(label = "Focal Length", value = meta.focalLength + if (meta.focalLength35mm.isNotEmpty()) " (${meta.focalLength35mm})" else "")
                            if (meta.flash.isNotEmpty()) DetailRow(label = "Flash", value = meta.flash)
                            if (meta.whiteBalance.isNotEmpty()) DetailRow(label = "White Balance", value = meta.whiteBalance)
                            if (meta.colorSpace.isNotEmpty()) DetailRow(label = "Color Space", value = meta.colorSpace)
                            if (meta.gpsCoordinates.isNotEmpty()) DetailRow(label = "GPS Coordinates", value = meta.gpsCoordinates)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = textColor.copy(alpha = 0.7f),
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(0.6f)
        )
    }
}
