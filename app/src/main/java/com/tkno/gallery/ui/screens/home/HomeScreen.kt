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
import androidx.compose.animation.core.tween
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
import com.tkno.gallery.ui.components.ClassicBottomBar
import com.tkno.gallery.ui.components.FastScrollbar
import com.tkno.gallery.ui.components.FloatingBottomBar
import com.tkno.gallery.ui.components.MediaGridItem
import androidx.compose.foundation.isSystemInDarkTheme
import com.tkno.gallery.theme.TaskbarActivePrimaryDark
import com.tkno.gallery.theme.TaskbarActivePrimaryLight
import com.tkno.gallery.theme.TaskbarInactiveVariantDark
import com.tkno.gallery.theme.TaskbarInactiveVariantLight
import com.tkno.gallery.theme.TaskbarIndicatorCapsuleDark
import com.tkno.gallery.theme.TaskbarIndicatorCapsuleLight
import com.tkno.gallery.util.FormatUtils
import com.tkno.gallery.ui.screens.menu.MenuScreen
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Stable
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

@Stable
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
    focusedMediaId: Long? = null,
    onItemClick: (MediaItem, List<MediaItem>) -> Unit,
    onVideoShortsClick: ((MediaItem, List<MediaItem>) -> Unit)? = null,
    onAlbumClick: (Album) -> Unit = {},
    onNavigateToRoute: (String) -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onAddFavorites: (Collection<String>) -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val exportFavoritesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val favItems = mediaItems.filter { it.isFavorite }
                    val content = favItems.joinToString("\n") { it.name }
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(content.toByteArray(Charsets.UTF_8))
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Exported ${favItems.size} favorites", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val importFavoritesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val lines = context.contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { seq ->
                        seq.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                    } ?: emptySet()

                    if (lines.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "File is empty", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    val matchedUris = mediaItems.filter { item ->
                        lines.any { line ->
                            item.name.equals(line, ignoreCase = true) ||
                            item.path.equals(line, ignoreCase = true) ||
                            item.path.endsWith("/$line", ignoreCase = true) ||
                            item.uri.toString().equals(line, ignoreCase = true)
                        }
                    }.map { it.uri.toString() }

                    if (matchedUris.isNotEmpty()) {
                        onAddFavorites(matchedUris)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Imported ${matchedUris.size} favorites", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "No matching media files found in gallery", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Import failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val prefs = remember { context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE) }

    var useClassicTaskbar by remember {
        mutableStateOf(prefs.getBoolean("use_classic_taskbar", false))
    }

    var showResolutionBadges by remember {
        mutableStateOf(prefs.getBoolean("show_resolution_badges", false))
    }

    var isShortsModeActive by rememberSaveable {
        mutableStateOf(prefs.getBoolean("video_shorts_mode", false))
    }

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == "use_classic_taskbar") {
                useClassicTaskbar = p.getBoolean("use_classic_taskbar", false)
            } else if (key == "show_resolution_badges") {
                showResolutionBadges = p.getBoolean("show_resolution_badges", false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

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
    var showMoreMenu by remember { mutableStateOf(false) }

    LaunchedEffect(currentTab) {
        isSelectionMode = false
        selectedItemIds.clear()
    }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedItemIds.clear()
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

    val galleryGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    val albumsGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    val videosGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    val imagesGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }

    fun findGridIndexForMediaId(groupedMedia: Map<String, List<MediaItem>>, targetMediaId: Long): Int? {
        var currentIndex = 0
        for ((_, items) in groupedMedia) {
            currentIndex++ // header item takes 1 slot
            for (item in items) {
                if (item.id == targetMediaId) {
                    return currentIndex
                }
                currentIndex++
            }
        }
        return null
    }

    // Instant & seamless synchronization of grid position with the currently viewed media item in full-screen pager
    LaunchedEffect(focusedMediaId) {
        val targetId = focusedMediaId ?: return@LaunchedEffect
        val (stateToScroll, groupedMap) = when (currentTab) {
            Tab.Media -> Pair(galleryGridState, allGrouped)
            Tab.Videos -> Pair(videosGridState, videosGrouped)
            Tab.Images -> Pair(imagesGridState, imagesGrouped)
            Tab.Console -> Pair(null, null)
        }
        if (stateToScroll != null && groupedMap != null) {
            val gridIndex = findGridIndexForMediaId(groupedMap, targetId)
            if (gridIndex != null) {
                val visibleIndices = stateToScroll.layoutInfo.visibleItemsInfo.map { it.index }
                if (visibleIndices.isEmpty() || gridIndex !in visibleIndices) {
                    stateToScroll.scrollToItem(gridIndex, 0)
                }
            }
        }
    }

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
    val activeColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Grid content (fills the whole screen edge to edge behind top and bottom floating bars)
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
                    onItemClick = { item, list ->
                        if (isShortsModeActive && onVideoShortsClick != null) {
                            onVideoShortsClick(item, list)
                        } else {
                            onItemClick(item, list)
                        }
                    },
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

        // 2. Floating Top Bar Capsules (Left & Right) with NO solid bar background
        val selectedCount = selectedItemIds.size
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Capsule: [ Menu Button + Title ("Gallery", "Albums", "Videos", "Images") ]
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                shape = RoundedCornerShape(percent = 50),
                tonalElevation = 0.dp,
                shadowElevation = 6.dp,
                modifier = Modifier.height(46.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp, end = 16.dp)
                ) {
                    if (isSelectionMode) {
                        IconButton(
                            onClick = {
                                isSelectionMode = false
                                selectedItemIds.clear()
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Selection",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = CustomIcons.LeftPanelOpen,
                                contentDescription = "Open Drawer",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Right Capsule: [ Heart Icon + 3-dots Menu Icon ] (Hidden on Albums tab)
            if (currentTab != Tab.Console) {
                Surface(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(percent = 50),
                    tonalElevation = 0.dp,
                    shadowElevation = 6.dp,
                    modifier = Modifier.height(46.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        if (isSelectionMode) {
                            // Action 1: Share
                            IconButton(
                                onClick = { shareSelectedItems() },
                                enabled = selectedCount > 0,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = if (selectedCount > 0) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Action 2: Trash / Delete
                            IconButton(
                                onClick = { deleteSelectedItems() },
                                enabled = selectedCount > 0,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = CustomIcons.Delete,
                                    contentDescription = "Trash",
                                    tint = if (selectedCount > 0) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Action 3: 3-dots Menu
                            Box {
                                IconButton(
                                    onClick = { showMoreMenu = true },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More Options",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                val currentItems = when (currentTab) {
                                    Tab.Media -> filteredMediaItems
                                    Tab.Videos -> filteredMediaItems.filter { it.isVideo }
                                    Tab.Images -> filteredMediaItems.filter { !it.isVideo }
                                    else -> filteredMediaItems
                                }
                                val isAllSelected = selectedItemIds.size == currentItems.size && currentItems.isNotEmpty()

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
                                            val allIds = currentItems.map { it.id }
                                            if (selectedItemIds.containsAll(allIds)) {
                                                selectedItemIds.clear()
                                            } else {
                                                selectedItemIds.clear()
                                                selectedItemIds.addAll(allIds)
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
                                            onNavigateToRoute("trash")
                                        }
                                    )
                                }
                            }
                        } else {
                            if (currentTab == Tab.Videos) {
                                IconButton(
                                    onClick = {
                                        isShortsModeActive = !isShortsModeActive
                                        prefs.edit().putBoolean("video_shorts_mode", isShortsModeActive).apply()
                                    },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = CustomIcons.KeyboardDoubleArrowDown,
                                        contentDescription = "TikTok Scrolling Video Mode",
                                        tint = if (isShortsModeActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            if (currentTab == Tab.Media || currentTab == Tab.Videos || currentTab == Tab.Images) {
                                IconButton(
                                    onClick = {
                                        isFavoriteFilterActive = !isFavoriteFilterActive
                                    },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFavoriteFilterActive) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Favorites Only",
                                        tint = if (isFavoriteFilterActive) Color.Red else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Box {
                                IconButton(
                                    onClick = { showMoreMenu = true },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More Options",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false }
                                ) {
                                    if (isFavoriteFilterActive) {
                                        DropdownMenuItem(
                                            text = { Text("Export favorites") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Outlined.UploadFile,
                                                    contentDescription = null
                                                )
                                            },
                                            onClick = {
                                                showMoreMenu = false
                                                val favCount = mediaItems.count { it.isFavorite }
                                                if (favCount == 0) {
                                                    Toast.makeText(context, "No favorites to export", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                                    exportFavoritesLauncher.launch("favorites_$dateStr.txt")
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Import favorites") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Outlined.FileDownload,
                                                    contentDescription = null
                                                )
                                            },
                                            onClick = {
                                                showMoreMenu = false
                                                importFavoritesLauncher.launch(arrayOf("text/plain", "text/*", "*/*"))
                                            }
                                        )
                                    }

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
                    }
                }
            }
        }

        // 3. Bottom Bar (Classic full-width NavigationBar or Floating Pill Dock)
        AnimatedVisibility(
            visible = !isSelectionMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(250)),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            if (useClassicTaskbar) {
                ClassicBottomBar(
                    tabs = reorderableTabs,
                    currentTab = currentTab,
                    onTabSelect = { currentTab = it },
                    onOrderChanged = { newOrder ->
                        saveNavOrder(newOrder)
                    }
                )
            } else {
                FloatingBottomBar(
                    tabs = reorderableTabs,
                    currentTab = currentTab,
                    onTabSelect = { currentTab = it },
                    onOrderChanged = { newOrder ->
                        saveNavOrder(newOrder)
                    }
                )
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
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE) }
    var showResolutionBadges by remember {
        mutableStateOf(prefs.getBoolean("show_resolution_badges", false))
    }
    var cardRoundedCorners by remember {
        mutableStateOf(prefs.getBoolean("card_rounded_corners", false))
    }

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == "show_resolution_badges") {
                showResolutionBadges = p.getBoolean("show_resolution_badges", false)
            } else if (key == "card_rounded_corners") {
                cardRoundedCorners = p.getBoolean("card_rounded_corners", false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

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
                            val changes = event.changes
                            var count = 0
                            var p1: androidx.compose.ui.input.pointer.PointerInputChange? = null
                            var p2: androidx.compose.ui.input.pointer.PointerInputChange? = null
                            for (i in 0 until changes.size) {
                                val change = changes[i]
                                if (change.pressed) {
                                    count++
                                    if (count == 1) p1 = change
                                    else if (count == 2) {
                                        p2 = change
                                        break
                                    }
                                }
                            }
                            if (count >= 2 && p1 != null && p2 != null) {
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
                                    for (i in 0 until changes.size) {
                                        changes[i].consume()
                                    }
                                }
                            } else {
                                accumulatedZoom = 1f
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            val sidePadding = when {
                columnCount >= 10 -> 1.dp
                columnCount >= 7 -> 2.dp
                else -> 4.dp
            }
            val topPadding = 64.dp + WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val bottomPadding = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnCount),
                state = state,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = sidePadding,
                    end = sidePadding,
                    top = topPadding,
                    bottom = bottomPadding
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
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .padding(
                                    top = 12.dp,
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
                            showResolutionBadge = showResolutionBadges,
                            roundedCorners = cardRoundedCorners,
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
        val topPadding = 64.dp + WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottomPadding = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = topPadding,
                bottom = bottomPadding
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(albums, key = { it.id }, contentType = { "album" }) { album ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            onAlbumClick(album)
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

                            if (album.isOnSdCard) {
                                Icon(
                                    imageVector = CustomIcons.SdCard,
                                    contentDescription = "SD Card",
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(20.dp)
                                        .align(Alignment.TopEnd),
                                    tint = Color.White
                                )
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

