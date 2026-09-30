package com.tkno.gallery.ui.screens.viewer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
    onCurrentItemChanged: ((MediaItem) -> Unit)? = null,
    onToggleFavorite: ((MediaItem) -> Unit)? = null,
    onDeleteMediaItem: ((MediaItem) -> Unit)? = null,
    onEditClick: ((MediaItem) -> Unit)? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    if (mediaItems.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) Color.Black else MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    // Intercept system back button / gesture to consistently execute onBackClick
    BackHandler {
        onBackClick()
    }

    val startIndex = initialIndex.coerceIn(0, (mediaItems.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { mediaItems.size })

    // Keep pager synchronized if initialIndex changes externally
    LaunchedEffect(initialIndex) {
        val target = initialIndex.coerceIn(0, (mediaItems.size - 1).coerceAtLeast(0))
        if (mediaItems.isNotEmpty() && pagerState.currentPage != target) {
            try {
                pagerState.scrollToPage(target)
            } catch (_: Throwable) {}
        }
    }

    LaunchedEffect(pagerState.currentPage, mediaItems) {
        val current = mediaItems.getOrNull(pagerState.currentPage)
        if (current != null) {
            onCurrentItemChanged?.invoke(current)
        }
    }
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() as? ComponentActivity }

    val prefs = remember(context) { context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE) }
    var useClassicViewerBar by remember { mutableStateOf(prefs.getBoolean("classic_viewer_bar", false)) }

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == "classic_viewer_bar") {
                useClassicViewerBar = p.getBoolean("classic_viewer_bar", false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
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

    LaunchedEffect(mediaItems.size) {
        if (mediaItems.isNotEmpty() && pagerState.currentPage >= mediaItems.size) {
            pagerState.scrollToPage(mediaItems.size - 1)
        }
    }

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

    val window = activity?.window
    val insetsController = remember(window) {
        window?.let { WindowCompat.getInsetsController(it, it.decorView) }
    }

    DisposableEffect(showBars, insetsController, isInPipMode) {
        insetsController?.let { controller ->
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (showBars && !isInPipMode) {
                controller.show(WindowInsetsCompat.Type.statusBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.statusBars())
            }
        }
        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    var showExifSheet by remember { mutableStateOf(false) }
    var exifData by remember { mutableStateOf<ExifData?>(null) }
    var isZoomedIn by remember { mutableStateOf(false) }
    var resetZoomTrigger by remember { mutableIntStateOf(0) }
    var captureFrameTrigger by remember { mutableIntStateOf(0) }

    val currentItem = mediaItems.getOrNull(pagerState.currentPage)
        ?: mediaItems.getOrNull(startIndex)
        ?: mediaItems.lastOrNull()

    if (currentItem == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) Color.Black else MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    fun deleteCurrentItem() {
        val itemToDelete = currentItem
        pendingDeleteItem = itemToDelete
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intentSender = MediaStore.createTrashRequest(
                    context.contentResolver,
                    listOf(itemToDelete.uri),
                    true
                ).intentSender
                deleteLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            } catch (e: Exception) {
                e.printStackTrace()
                pendingDeleteItem = null
            }
        } else {
            try {
                val rows = context.contentResolver.delete(itemToDelete.uri, null, null)
                if (rows > 0) {
                    onDeleteMediaItem?.invoke(itemToDelete)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingDeleteItem = null
            }
        }
    }

    LaunchedEffect(currentItem.uri) {
        if (!currentItem.isVideo) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                exifData = ExifUtils.readExif(context, currentItem.uri)
            }
        } else {
            exifData = null
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        isZoomedIn = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isZoomedIn,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 16.dp,
            key = { index -> mediaItems.getOrNull(index)?.uri?.toString() ?: index.toString() }
        ) { page ->
            val item = mediaItems[page]
            val isCurrentPage = (pagerState.currentPage == page)

            var pageModifier = Modifier.fillMaxSize()
            if (isCurrentPage && sharedTransitionScope != null && animatedVisibilityScope != null && item.id > 0L) {
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
                            } else null,
                            captureFrameTrigger = captureFrameTrigger
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (isDark) Color.Black else MaterialTheme.colorScheme.background)
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

        // Top Floating Action Controls (Back Circle & Capsule: FitScreen + Rotate + More)
        AnimatedVisibility(
            visible = showBars && !isInPipMode,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val topCapsuleBg = if (isDark) Color.Black.copy(alpha = 0.5f) else MaterialTheme.colorScheme.background.copy(alpha = 0.88f)
                    val topCapsuleContent = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                    val topCapsuleElevation = if (isDark) 0.dp else 4.dp

                    // Back Button in a Circle
                    Surface(
                        shape = CircleShape,
                        color = topCapsuleBg,
                        contentColor = topCapsuleContent,
                        shadowElevation = topCapsuleElevation,
                        modifier = Modifier.size(44.dp)
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = CustomIcons.ChevronLeft,
                                contentDescription = "Back",
                                tint = topCapsuleContent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Capsule containing FitScreen (when zoomed), Rotate, and Three Dots (More)
                    Surface(
                        shape = CircleShape,
                        color = topCapsuleBg,
                        contentColor = topCapsuleContent,
                        shadowElevation = topCapsuleElevation,
                        modifier = Modifier
                            .height(44.dp)
                            .animateContentSize(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            // FitScreen Button (Only visible when image is zoomed in, placed to the left of Rotate)
                            AnimatedVisibility(
                                visible = !currentItem.isVideo && isZoomedIn,
                                enter = fadeIn() + expandHorizontally(),
                                exit = fadeOut() + shrinkHorizontally()
                            ) {
                                IconButton(
                                    onClick = {
                                        resetZoomTrigger++
                                        isZoomedIn = false
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = CustomIcons.FitScreen,
                                        contentDescription = "Fit to Screen",
                                        tint = topCapsuleContent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Capture Video Frame Button (Only visible when current media is video, placed to the left of Rotate)
                            AnimatedVisibility(
                                visible = currentItem.isVideo,
                                enter = fadeIn() + expandHorizontally(),
                                exit = fadeOut() + shrinkHorizontally()
                            ) {
                                IconButton(
                                    onClick = {
                                        captureFrameTrigger++
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = CustomIcons.ScreenshotFrame2,
                                        contentDescription = "Capture Frame",
                                        tint = topCapsuleContent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Rotate Screen Button
                            IconButton(
                                onClick = {
                                    val act = context.findActivity() ?: return@IconButton
                                    if (isLandscape) {
                                        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    } else {
                                        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = CustomIcons.MobileRotate,
                                    contentDescription = "Rotate Screen",
                                    tint = if (isLandscape) MaterialTheme.colorScheme.primary else topCapsuleContent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Three Dots Button (to be customized later)
                            IconButton(
                                onClick = {
                                    // Three dots action will be customized later
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = topCapsuleContent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action Bar (Classic or Capsule based on preference)
        AnimatedVisibility(
            visible = showBars && !isInPipMode,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = if (useClassicViewerBar) {
                Modifier.align(Alignment.BottomCenter)
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
            }
        ) {
            val barBgColor = if (isDark) {
                if (useClassicViewerBar) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.background.copy(alpha = if (useClassicViewerBar) 0.94f else 0.88f)
            }
            val barContentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
            val barElevation = if (isDark) 0.dp else 6.dp

            Surface(
                color = barBgColor,
                shape = if (useClassicViewerBar) RoundedCornerShape(0.dp) else CircleShape,
                contentColor = barContentColor,
                shadowElevation = barElevation,
                modifier = if (useClassicViewerBar) Modifier.fillMaxWidth() else Modifier.height(64.dp)
            ) {
                Row(
                    modifier = if (useClassicViewerBar) {
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .height(72.dp)
                    } else {
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp)
                    },
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = currentItem.mimeType
                                    putExtra(Intent.EXTRA_STREAM, currentItem.uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                            }
                            .padding(horizontal = if (useClassicViewerBar) 16.dp else 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = CustomIcons.Share,
                            contentDescription = "Share",
                            tint = barContentColor,
                            modifier = Modifier.size(if (useClassicViewerBar) 24.dp else 22.dp)
                        )
                        Spacer(modifier = Modifier.height(if (useClassicViewerBar) 4.dp else 2.dp))
                        Text(
                            text = "Share",
                            color = barContentColor,
                            fontSize = if (useClassicViewerBar) 12.sp else 11.sp
                        )
                    }

                    // Auto (Images only)
                    if (!currentItem.isVideo) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = CustomIcons.WandStars,
                                contentDescription = "Auto",
                                tint = barContentColor,
                                modifier = Modifier.size(if (useClassicViewerBar) 24.dp else 22.dp)
                            )
                            Spacer(modifier = Modifier.height(if (useClassicViewerBar) 4.dp else 2.dp))
                            Text(
                                text = "Auto",
                                color = barContentColor,
                                fontSize = if (useClassicViewerBar) 12.sp else 11.sp
                            )
                        }
                    }

                    // Favorite
                    val isFav = currentItem.isFavorite
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onToggleFavorite?.invoke(currentItem) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Filled.Favorite else CustomIcons.Favorite,
                            contentDescription = if (isFav) "Unfavorite" else "Favorite",
                            tint = if (isFav) Color.Red else barContentColor,
                            modifier = Modifier.size(if (useClassicViewerBar) 24.dp else 22.dp)
                        )
                        Spacer(modifier = Modifier.height(if (useClassicViewerBar) 4.dp else 2.dp))
                        Text(
                            text = if (isFav) "Unfavorite" else "Favorite",
                            color = barContentColor,
                            fontSize = if (useClassicViewerBar) 12.sp else 11.sp
                        )
                    }

                    // Edit
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onEditClick?.invoke(currentItem) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = CustomIcons.Edit,
                            contentDescription = "Edit",
                            tint = barContentColor,
                            modifier = Modifier.size(if (useClassicViewerBar) 24.dp else 22.dp)
                        )
                        Spacer(modifier = Modifier.height(if (useClassicViewerBar) 4.dp else 2.dp))
                        Text(
                            text = "Edit",
                            color = barContentColor,
                            fontSize = if (useClassicViewerBar) 12.sp else 11.sp
                        )
                    }

                    // Trash
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { deleteCurrentItem() }
                            .padding(horizontal = if (useClassicViewerBar) 16.dp else 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = CustomIcons.Delete,
                            contentDescription = "Trash",
                            tint = barContentColor,
                            modifier = Modifier.size(if (useClassicViewerBar) 24.dp else 22.dp)
                        )
                        Spacer(modifier = Modifier.height(if (useClassicViewerBar) 4.dp else 2.dp))
                        Text(
                            text = "Trash",
                            color = barContentColor,
                            fontSize = if (useClassicViewerBar) 12.sp else 11.sp
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
