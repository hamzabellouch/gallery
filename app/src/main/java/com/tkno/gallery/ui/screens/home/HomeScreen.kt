package com.tkno.gallery.ui.screens.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.input.pointer.PointerEventPass
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

enum class GalleryGridLevel(val columnCount: Int) {
    Large(3),   // 3 items / row - Days
    Medium(4),  // 4 items / row - Days
    Month(7),   // 7 items / row - Months
    Year(10);   // 10 items / row - Years

    fun zoomIn(): GalleryGridLevel = when (this) {
        Year -> Month
        Month -> Medium
        Medium -> Large
        Large -> Large
    }

    fun zoomOut(): GalleryGridLevel = when (this) {
        Large -> Medium
        Medium -> Month
        Month -> Year
        Year -> Year
    }
}

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
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val prefs = remember { context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE) }

    val savedLevelName = prefs.getString("gallery_grid_level", GalleryGridLevel.Large.name)
    var gridLevel by rememberSaveable {
        mutableStateOf(
            try { GalleryGridLevel.valueOf(savedLevelName ?: GalleryGridLevel.Large.name) }
            catch (e: Exception) { GalleryGridLevel.Large }
        )
    }

    fun zoomIn() {
        val next = gridLevel.zoomIn()
        if (next != gridLevel) {
            gridLevel = next
            prefs.edit().putString("gallery_grid_level", next.name).apply()
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun zoomOut() {
        val next = gridLevel.zoomOut()
        if (next != gridLevel) {
            gridLevel = next
            prefs.edit().putString("gallery_grid_level", next.name).apply()
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    var currentTab by rememberSaveable { mutableStateOf(Tab.Media) }
    var isFavoriteFilterActive by rememberSaveable { mutableStateOf(false) }

    val filteredMediaItems = remember(mediaItems, isFavoriteFilterActive) {
        if (isFavoriteFilterActive) {
            mediaItems.filter { it.isFavorite }
        } else {
            mediaItems
        }
    }

    val allGrouped = remember(filteredMediaItems, gridLevel) {
        when (gridLevel) {
            GalleryGridLevel.Large, GalleryGridLevel.Medium ->
                filteredMediaItems.groupBy { FormatUtils.formatDateHeader(it.dateAddedSec) }
            GalleryGridLevel.Month ->
                filteredMediaItems.groupBy { FormatUtils.formatMonthHeader(it.dateAddedSec) }
            GalleryGridLevel.Year ->
                filteredMediaItems.groupBy { FormatUtils.formatYearHeader(it.dateAddedSec) }
        }
    }

    val imagesGrouped = remember(filteredMediaItems, gridLevel) {
        val items = filteredMediaItems.filter { !it.isVideo }
        when (gridLevel) {
            GalleryGridLevel.Large, GalleryGridLevel.Medium ->
                items.groupBy { FormatUtils.formatDateHeader(it.dateAddedSec) }
            GalleryGridLevel.Month ->
                items.groupBy { FormatUtils.formatMonthHeader(it.dateAddedSec) }
            GalleryGridLevel.Year ->
                items.groupBy { FormatUtils.formatYearHeader(it.dateAddedSec) }
        }
    }

    val videosGrouped = remember(filteredMediaItems, gridLevel) {
        val items = filteredMediaItems.filter { it.isVideo }
        when (gridLevel) {
            GalleryGridLevel.Large, GalleryGridLevel.Medium ->
                items.groupBy { FormatUtils.formatDateHeader(it.dateAddedSec) }
            GalleryGridLevel.Month ->
                items.groupBy { FormatUtils.formatMonthHeader(it.dateAddedSec) }
            GalleryGridLevel.Year ->
                items.groupBy { FormatUtils.formatYearHeader(it.dateAddedSec) }
        }
    }

    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    val selectedItemIds = remember { mutableStateListOf<Long>() }
    val selectedAlbumIds = remember { mutableStateListOf<String>() }
    var showMoreMenu by remember { mutableStateOf(false) }

    LaunchedEffect(currentTab) {
        isSelectionMode = false
        selectedItemIds.clear()
        selectedAlbumIds.clear()
    }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedItemIds.clear()
        selectedAlbumIds.clear()
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isSelectionMode = false
            selectedItemIds.clear()
        }
    }

    fun shareSelectedItems() {
        val selectedMedia = filteredMediaItems.filter { it.id in selectedItemIds }
        if (selectedMedia.isEmpty()) return
        val uris = ArrayList(selectedMedia.map { it.uri })
        val intent = Intent().apply {
            if (uris.size == 1) {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uris[0])
                val firstItem = selectedMedia.firstOrNull()
                val mime = firstItem?.mimeType ?: ""
                type = if (mime.isNotBlank()) mime else if (firstItem?.isVideo == true) "video/*" else "image/*"
            } else {
                action = Intent.ACTION_SEND_MULTIPLE
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                type = "*/*"
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }

    fun deleteSelectedItems() {
        val selectedMedia = filteredMediaItems.filter { it.id in selectedItemIds }
        if (selectedMedia.isEmpty()) return
        val uris = selectedMedia.map { it.uri }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intentSender = MediaStore.createTrashRequest(context.contentResolver, uris, true).intentSender
                deleteLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                uris.forEach { uri ->
                    context.contentResolver.delete(uri, null, null)
                }
                selectedItemIds.clear()
                isSelectionMode = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleItemSelection(id: Long) {
        if (selectedItemIds.contains(id)) {
            selectedItemIds.remove(id)
        } else {
            selectedItemIds.add(id)
        }
    }

    fun enterSelectionMode(initialId: Long) {
        isSelectionMode = true
        selectedItemIds.clear()
        selectedItemIds.add(initialId)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun toggleAlbumSelection(id: String) {
        if (selectedAlbumIds.contains(id)) {
            selectedAlbumIds.remove(id)
        } else {
            selectedAlbumIds.add(id)
        }
    }

    fun enterAlbumSelectionMode(initialId: String) {
        isSelectionMode = true
        selectedAlbumIds.clear()
        selectedAlbumIds.add(initialId)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val galleryGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    val albumsGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    val videosGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    val imagesGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }

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

    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
    val indicatorCapsuleColor = MaterialTheme.colorScheme.secondaryContainer
    val navContainerColor = MaterialTheme.colorScheme.background

    Scaffold(
        topBar = {
            val selectedCount = if (currentTab == Tab.Console) selectedAlbumIds.size else selectedItemIds.size
            TopAppBar(
                title = {
                    Text(
                        text = if (isSelectionMode) {
                            "$selectedCount"
                        } else {
                            when (currentTab) {
                                Tab.Media -> "Gallery"
                                Tab.Console -> "Albums"
                                Tab.Videos -> "Videos"
                                Tab.Images -> "Images"
                            }
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(
                            onClick = {
                                isSelectionMode = false
                                selectedItemIds.clear()
                                selectedAlbumIds.clear()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Selection",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(
                                imageVector = CustomIcons.LeftPanelOpen,
                                contentDescription = "Open Drawer",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        // Action 1: Share
                        IconButton(
                            onClick = { shareSelectedItems() },
                            enabled = selectedCount > 0 && currentTab != Tab.Console
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = if (selectedCount > 0 && currentTab != Tab.Console) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                }
                            )
                        }

                        // Action 2: Trash / Delete
                        IconButton(
                            onClick = { deleteSelectedItems() },
                            enabled = selectedCount > 0 && currentTab != Tab.Console
                        ) {
                            Icon(
                                imageVector = CustomIcons.Delete,
                                contentDescription = "Trash",
                                tint = if (selectedCount > 0 && currentTab != Tab.Console) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                }
                            )
                        }

                        // Action 3: 3-dots Menu
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            val isAllSelected = if (currentTab == Tab.Console) {
                                selectedAlbumIds.size == albums.size && albums.isNotEmpty()
                            } else {
                                val currentItems = when (currentTab) {
                                    Tab.Media -> filteredMediaItems
                                    Tab.Videos -> filteredMediaItems.filter { it.isVideo }
                                    Tab.Images -> filteredMediaItems.filter { !it.isVideo }
                                    else -> filteredMediaItems
                                }
                                selectedItemIds.size == currentItems.size && currentItems.isNotEmpty()
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isAllSelected) "Deselect all" else "Select all") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.SelectAll,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        if (currentTab == Tab.Console) {
                                            val allAlbumIds = albums.map { it.id }
                                            if (selectedAlbumIds.containsAll(allAlbumIds)) {
                                                selectedAlbumIds.clear()
                                            } else {
                                                selectedAlbumIds.clear()
                                                selectedAlbumIds.addAll(allAlbumIds)
                                            }
                                        } else {
                                            val currentItems = when (currentTab) {
                                                Tab.Media -> filteredMediaItems
                                                Tab.Videos -> filteredMediaItems.filter { it.isVideo }
                                                Tab.Images -> filteredMediaItems.filter { !it.isVideo }
                                                else -> filteredMediaItems
                                            }
                                            val allIds = currentItems.map { it.id }
                                            if (selectedItemIds.containsAll(allIds)) {
                                                selectedItemIds.clear()
                                            } else {
                                                selectedItemIds.clear()
                                                selectedItemIds.addAll(allIds)
                                            }
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Trash") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = CustomIcons.Delete,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        isSelectionMode = false
                                        selectedItemIds.clear()
                                        selectedAlbumIds.clear()
                                        onNavigateToRoute("trash")
                                    }
                                )
                            }
                        }
                    } else {
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
                        }

                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Select items") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        isSelectionMode = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Trash") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = CustomIcons.Delete,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        onNavigateToRoute("trash")
                                    }
                                )
                            }
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
                        gridLevel = gridLevel,
                        isSelectionMode = isSelectionMode,
                        selectedItemIds = selectedItemIds,
                        onToggleSelect = ::toggleItemSelection,
                        onEnterSelectionMode = ::enterSelectionMode,
                        onZoomIn = ::zoomIn,
                        onZoomOut = ::zoomOut,
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
                        isSelectionMode = isSelectionMode,
                        selectedAlbumIds = selectedAlbumIds,
                        onToggleSelect = ::toggleAlbumSelection,
                        onEnterSelectionMode = ::enterAlbumSelectionMode,
                        onAlbumClick = onAlbumClick
                    )
                }
                Tab.Videos -> {
                    MediaGridContent(
                        groupedMedia = videosGrouped,
                        gridLevel = gridLevel,
                        isSelectionMode = isSelectionMode,
                        selectedItemIds = selectedItemIds,
                        onToggleSelect = ::toggleItemSelection,
                        onEnterSelectionMode = ::enterSelectionMode,
                        onZoomIn = ::zoomIn,
                        onZoomOut = ::zoomOut,
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
                        gridLevel = gridLevel,
                        isSelectionMode = isSelectionMode,
                        selectedItemIds = selectedItemIds,
                        onToggleSelect = ::toggleItemSelection,
                        onEnterSelectionMode = ::enterSelectionMode,
                        onZoomIn = ::zoomIn,
                        onZoomOut = ::zoomOut,
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
    gridLevel: GalleryGridLevel,
    isSelectionMode: Boolean = false,
    selectedItemIds: List<Long> = emptyList(),
    onToggleSelect: (Long) -> Unit = {},
    onEnterSelectionMode: (Long) -> Unit = {},
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    state: LazyGridState = rememberLazyGridState(),
    onItemClick: (MediaItem, List<MediaItem>) -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val fullFlattenedList = remember(groupedMedia) {
        groupedMedia.values.flatten()
    }

    val columnCount = gridLevel.columnCount

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        var accumulatedZoom = 1f
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val pressedChanges = event.changes.filter { it.pressed }
                            if (pressedChanges.size >= 2) {
                                val p1 = pressedChanges[0]
                                val p2 = pressedChanges[1]
                                val prevDistance = (p1.previousPosition - p2.previousPosition).getDistance()
                                val currDistance = (p1.position - p2.position).getDistance()
                                if (prevDistance > 0f && currDistance > 0f) {
                                    val zoom = currDistance / prevDistance
                                    accumulatedZoom *= zoom
                                    if (accumulatedZoom > 1.25f) {
                                        onZoomIn()
                                        accumulatedZoom = 1f
                                    } else if (accumulatedZoom < 0.80f) {
                                        onZoomOut()
                                        accumulatedZoom = 1f
                                    }
                                    pressedChanges.forEach { it.consume() }
                                }
                            } else {
                                accumulatedZoom = 1f
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnCount),
                state = state,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    when {
                        columnCount >= 10 -> 1.dp
                        columnCount >= 7 -> 2.dp
                        else -> 4.dp
                    }
                )
            ) {
                groupedMedia.forEach { (headerDate, itemsInGroup) ->
                    item(
                        span = { GridItemSpan(columnCount) },
                        key = "header_${gridLevel.name}_$headerDate",
                        contentType = "header"
                    ) {
                        Text(
                            text = headerDate,
                            fontSize = when {
                                columnCount >= 10 -> 18.sp
                                columnCount >= 7 -> 16.sp
                                else -> 14.sp
                            },
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .padding(
                                    top = when {
                                        columnCount >= 10 -> 16.dp
                                        columnCount >= 7 -> 14.dp
                                        else -> 12.dp
                                    },
                                    bottom = 6.dp
                                )
                        )
                    }

                    items(
                        itemsInGroup,
                        key = { it.id },
                        contentType = { if (it.isVideo) "video" else "image" }
                    ) { item ->
                        val isSelected = item.id in selectedItemIds
                        MediaGridItem(
                            item = item,
                            columnCount = columnCount,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onClick = {
                                if (isSelectionMode) {
                                    onToggleSelect(item.id)
                                } else {
                                    onItemClick(item, fullFlattenedList)
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    onEnterSelectionMode(item.id)
                                } else {
                                    onToggleSelect(item.id)
                                }
                            },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                }
            }

            FastScrollbar(
                gridState = state,
                mediaItems = fullFlattenedList,
                groupedMedia = groupedMedia,
                gridLevel = gridLevel,
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
    isSelectionMode: Boolean = false,
    selectedAlbumIds: List<String> = emptyList(),
    onToggleSelect: (String) -> Unit = {},
    onEnterSelectionMode: (String) -> Unit = {},
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
            items(albums, key = { it.id }, contentType = { "album" }) { album ->
                val isSelected = album.id in selectedAlbumIds
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .let { mod ->
                            if (isSelectionMode && isSelected) {
                                mod.border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                            } else mod
                        }
                        .clickable {
                            if (isSelectionMode) {
                                onToggleSelect(album.id)
                            } else {
                                onAlbumClick(album)
                            }
                        },
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

                            if (album.isOnSdCard && !isSelectionMode) {
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

                            if (isSelectionMode) {
                                Box(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(24.dp)
                                        .align(Alignment.TopStart)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else Color.Black.copy(alpha = 0.45f)
                                        )
                                        .let { mod ->
                                            if (!isSelected) {
                                                mod.border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                                            } else mod
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
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

