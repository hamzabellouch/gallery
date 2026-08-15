package com.tkno.gallery.ui.screens.home

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.tkno.gallery.data.model.Album
import com.tkno.gallery.data.model.MediaItem
import com.tkno.gallery.ui.components.AutoAwesomeMosaicIcon
import com.tkno.gallery.ui.components.CustomIcons
import com.tkno.gallery.ui.components.GalleryThumbnailIcon
import com.tkno.gallery.ui.components.ImageIcon
import com.tkno.gallery.ui.components.VideoTemplateIcon
import com.tkno.gallery.ui.components.FastScrollbar
import com.tkno.gallery.ui.components.MediaGridItem
import androidx.compose.foundation.isSystemInDarkTheme
import com.tkno.gallery.theme.TaskbarActivePrimaryDark
import com.tkno.gallery.theme.TaskbarActivePrimaryLight
import com.tkno.gallery.theme.TaskbarInactiveVariantDark
import com.tkno.gallery.theme.TaskbarInactiveVariantLight
import com.tkno.gallery.theme.TaskbarIndicatorCapsuleDark
import com.tkno.gallery.theme.TaskbarIndicatorCapsuleLight
import com.tkno.gallery.util.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.tkno.gallery.ui.screens.menu.MenuScreen

enum class Tab {
    Media, Console, Videos, Images
}

data class TabInfo(
    val tab: Tab,
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    mediaItems: List<MediaItem>,
    albums: List<Album> = emptyList(),
    onItemClick: (MediaItem, List<MediaItem>) -> Unit,
    onAlbumClick: (Album) -> Unit = {},
    onNavigateToRoute: (String) -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    var columnCount by remember { mutableIntStateOf(3) }
    var currentTab by rememberSaveable { mutableStateOf(Tab.Media) }
    var isFavoriteFilterActive by rememberSaveable { mutableStateOf(false) }

    val galleryGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    val albumsGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    val videosGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    val imagesGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val prefs = remember { context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE) }

    fun loadNavOrder(): List<Tab> {
        val allTabs = listOf(Tab.Media, Tab.Console, Tab.Videos, Tab.Images)
        val saved = prefs.getString("nav_tab_order", null) ?: return allTabs
        val names = saved.split(",")
        val list = names.mapNotNull { name ->
            try { Tab.valueOf(name) } catch (e: Exception) { null }
        }.distinct()
        val missing = allTabs.filter { it !in list }
        val result = list + missing
        return if (result.size == 4) result else allTabs
    }

    fun saveNavOrder(list: List<Tab>) {
        val saved = list.joinToString(",") { it.name }
        prefs.edit().putString("nav_tab_order", saved).apply()
    }

    val reorderableTabs = remember { mutableStateListOf(*loadNavOrder().toTypedArray()) }

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var currentDragOffset by remember { mutableStateOf(0f) }
    var itemWidthPx by remember { mutableStateOf(0f) }

    @Composable
    fun getTabInfo(tab: Tab): TabInfo {
        return when (tab) {
            Tab.Media -> TabInfo(Tab.Media, "Gallery", GalleryThumbnailIcon)
            Tab.Console -> TabInfo(Tab.Console, "Albums", AutoAwesomeMosaicIcon)
            Tab.Videos -> TabInfo(Tab.Videos, "Videos", VideoTemplateIcon)
            Tab.Images -> TabInfo(Tab.Images, "Images", ImageIcon)
        }
    }

    val filteredMediaItems = remember(mediaItems, isFavoriteFilterActive) {
        if (isFavoriteFilterActive) {
            mediaItems.filter { it.isFavorite }
        } else {
            mediaItems
        }
    }

    val allGrouped = remember(filteredMediaItems) {
        filteredMediaItems.groupBy { FormatUtils.formatDateHeader(it.dateAddedSec) }
    }

    val imagesGrouped = remember(filteredMediaItems) {
        filteredMediaItems.filter { !it.isVideo }.groupBy { FormatUtils.formatDateHeader(it.dateAddedSec) }
    }

    val videosGrouped = remember(filteredMediaItems) {
        filteredMediaItems.filter { it.isVideo }.groupBy { FormatUtils.formatDateHeader(it.dateAddedSec) }
    }

    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
    val indicatorCapsuleColor = MaterialTheme.colorScheme.secondaryContainer
    val navContainerColor = MaterialTheme.colorScheme.background

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentTab) {
                            Tab.Media -> "Gallery"
                            Tab.Console -> "Albums"
                            Tab.Videos -> "Videos"
                            Tab.Images -> "Images"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = CustomIcons.LeftPanelOpen,
                            contentDescription = "Open Drawer",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                    actions = {
                        if (currentTab == Tab.Media || currentTab == Tab.Videos || currentTab == Tab.Images) {
                            IconButton(onClick = {
                                isFavoriteFilterActive = !isFavoriteFilterActive
                            }) {
                                Icon(
                                    imageVector = if (isFavoriteFilterActive) Icons.Filled.Favorite else CustomIcons.Favorite,
                                    contentDescription = "Favorites Only",
                                    tint = if (isFavoriteFilterActive) Color.Red else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = {
                                columnCount = if (columnCount == 3) 4 else 3
                            }) {
                                Icon(
                                    imageVector = if (columnCount == 3) Icons.Default.Grid4x4 else Icons.Default.Grid3x3,
                                    contentDescription = "Grid Size"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
        bottomBar = {
            NavigationBar(
                containerColor = navContainerColor,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .height(72.dp)
                    .onGloballyPositioned { coordinates ->
                        val totalWidth = coordinates.size.width.toFloat()
                        if (totalWidth > 0) {
                            itemWidthPx = totalWidth / 4f
                        }
                    }
            ) {
                // Render the 4 reorderable tabs (Gallery, Albums, Videos, Images)
                reorderableTabs.forEachIndexed { index, tab ->
                    val info = getTabInfo(tab)
                    val isSelected = currentTab == tab
                    val isDragging = draggingIndex == index

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = info.icon,
                                contentDescription = info.label,
                                tint = if (isSelected) activeColor else inactiveColor
                            )
                        },
                        label = {
                            Text(
                                text = info.label,
                                color = if (isSelected) activeColor else inactiveColor,
                                fontSize = 12.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = indicatorCapsuleColor
                        ),
                        modifier = Modifier
                            .graphicsLayer {
                                if (isDragging) {
                                    translationX = currentDragOffset
                                    scaleX = 1.12f
                                    scaleY = 1.12f
                                }
                            }
                            .pointerInput(index) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    var isLongPressActive = false

                                    val longPressTimer = scope.launch {
                                        delay(viewConfiguration.longPressTimeoutMillis)
                                        isLongPressActive = true
                                        draggingIndex = index
                                        currentDragOffset = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }

                                    val pointer = down.id
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == pointer }

                                        if (change == null || !change.pressed) {
                                            longPressTimer.cancel()
                                            if (isLongPressActive) {
                                                draggingIndex = null
                                                currentDragOffset = 0f
                                            }
                                            break
                                        }

                                        if (!isLongPressActive) {
                                            val diff = change.position - down.position
                                            if (diff.getDistance() > viewConfiguration.touchSlop) {
                                                longPressTimer.cancel()
                                            }
                                        } else {
                                            change.consume()
                                            val deltaX = change.position.x - change.previousPosition.x
                                            val activeIndex = draggingIndex ?: break
                                            currentDragOffset += deltaX

                                            val threshold = if (itemWidthPx > 0f) itemWidthPx * 0.5f else 100f

                                            if (currentDragOffset > threshold && activeIndex < reorderableTabs.size - 1) {
                                                val temp = reorderableTabs[activeIndex]
                                                reorderableTabs[activeIndex] = reorderableTabs[activeIndex + 1]
                                                reorderableTabs[activeIndex + 1] = temp
                                                draggingIndex = activeIndex + 1
                                                currentDragOffset -= itemWidthPx
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                saveNavOrder(reorderableTabs)
                                            } else if (currentDragOffset < -threshold && activeIndex > 0) {
                                                val temp = reorderableTabs[activeIndex]
                                                reorderableTabs[activeIndex] = reorderableTabs[activeIndex - 1]
                                                reorderableTabs[activeIndex - 1] = temp
                                                draggingIndex = activeIndex - 1
                                                currentDragOffset += itemWidthPx
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                saveNavOrder(reorderableTabs)
                                            }
                                        }
                                    }
                                }
                            }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                Tab.Media -> {
                    MediaGridContent(
                        groupedMedia = allGrouped,
                        columnCount = columnCount,
                        state = galleryGridState,
                        onItemClick = onItemClick,
                        accentColor = activeColor,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
                Tab.Console -> {
                    ConsoleContent(
                        albums = albums,
                        state = albumsGridState,
                        onAlbumClick = onAlbumClick
                    )
                }
                Tab.Videos -> {
                    MediaGridContent(
                        groupedMedia = videosGrouped,
                        columnCount = columnCount,
                        state = videosGridState,
                        onItemClick = onItemClick,
                        accentColor = activeColor,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
                Tab.Images -> {
                    MediaGridContent(
                        groupedMedia = imagesGrouped,
                        columnCount = columnCount,
                        state = imagesGridState,
                        onItemClick = onItemClick,
                        accentColor = activeColor,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MediaGridContent(
    groupedMedia: Map<String, List<MediaItem>>,
    columnCount: Int,
    state: LazyGridState = rememberLazyGridState(),
    onItemClick: (MediaItem, List<MediaItem>) -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val fullFlattenedList = remember(groupedMedia) {
        groupedMedia.values.flatten()
    }

    if (groupedMedia.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No media files found.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnCount),
                state = state,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp)
            ) {
                groupedMedia.forEach { (headerDate, itemsInGroup) ->
                    item(span = { GridItemSpan(columnCount) }, key = "header_$headerDate") {
                        Text(
                            text = headerDate,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .padding(top = 12.dp, bottom = 6.dp)
                        )
                    }

                    items(itemsInGroup, key = { it.id }) { item ->
                        MediaGridItem(
                            item = item,
                            onClick = { onItemClick(item, fullFlattenedList) },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                }
            }

            FastScrollbar(
                gridState = state,
                mediaItems = fullFlattenedList,
                columnCount = columnCount,
                accentColor = accentColor,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
private fun ConsoleContent(
    albums: List<Album>,
    state: LazyGridState = rememberLazyGridState(),
    onAlbumClick: (Album) -> Unit
) {
    val context = LocalContext.current
    val iconColor = MaterialTheme.colorScheme.primary

    if (albums.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No albums found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(albums, key = { it.id }) { album ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAlbumClick(album) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        ) {
                            if (album.coverUri != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(album.coverUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = album.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .align(Alignment.Center),
                                    tint = iconColor
                                )
                            }

                            if (album.isOnSdCard) {
                                Surface(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .align(Alignment.TopEnd),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                                    shadowElevation = 2.dp
                                ) {
                                    Icon(
                                        imageVector = CustomIcons.SdCard,
                                        contentDescription = "SD Card",
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .size(18.dp),
                                        tint = iconColor
                                    )
                                }
                            }
                        }
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = album.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Text(
                                text = "${album.itemCount} items",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

