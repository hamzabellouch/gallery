package com.tkno.gallery.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.tkno.gallery.data.model.Album
import com.tkno.gallery.data.model.MediaItem
import com.tkno.gallery.data.repository.FavoritesManager
import com.tkno.gallery.data.repository.MediaStoreRepository
import com.tkno.gallery.ui.page.settings.about.UpdatePage
import com.tkno.gallery.ui.screens.editor.PhotoEditorScreen
import com.tkno.gallery.ui.screens.editor.VideoEditorScreen
import com.tkno.gallery.ui.screens.home.HomeScreen
import com.tkno.gallery.ui.screens.menu.*
import com.tkno.gallery.ui.screens.player.ShortsPlayerScreen
import com.tkno.gallery.ui.screens.trash.TrashScreen
import com.tkno.gallery.ui.screens.viewer.MediaPagerScreen
import kotlinx.coroutines.launch

import android.content.Intent
import android.provider.MediaStore
import androidx.compose.ui.platform.LocalContext
import com.tkno.gallery.util.MediaIntentResolver

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    mediaItems: List<MediaItem>,
    albums: List<Album>,
    favoritesManager: FavoritesManager,
    repository: MediaStoreRepository,
    incomingIntent: Intent? = null,
    onIntentConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val favoriteUris by favoritesManager.favoriteUris.collectAsState()

    val mediaItemsWithFavorites = remember(mediaItems, favoriteUris) {
        mediaItems.map { it.copy(isFavorite = it.uri.toString() in favoriteUris) }
    }

    var activeMediaList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var activeInitialIndex by remember { mutableIntStateOf(0) }
    var focusedMediaId by remember { mutableStateOf<Long?>(null) }
    var selectedAlbumItem by remember { mutableStateOf<Album?>(null) }
    var editingMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    var isExternalViewerMode by remember { mutableStateOf(false) }

    // Handle Incoming Intent (ACTION_VIEW, ACTION_SEND, ACTION_SEND_MULTIPLE, ACTION_REVIEW, etc.)
    LaunchedEffect(incomingIntent) {
        val intent = incomingIntent ?: return@LaunchedEffect
        if (!MediaIntentResolver.isMediaIntent(intent)) return@LaunchedEffect

        val resolvedList = MediaIntentResolver.resolveMediaItemsFromIntent(context, intent)
        if (resolvedList.isNotEmpty()) {
            isExternalViewerMode = true
            activeMediaList = resolvedList
            focusedMediaId = resolvedList[0].id
            activeInitialIndex = 0

            try {
                // Ensure NavHost is fully initialized before navigating
                while (navController.currentBackStackEntry == null) {
                    kotlinx.coroutines.delay(30)
                }
                if (navController.currentDestination?.route != "media_pager") {
                    navController.navigate("media_pager") {
                        launchSingleTop = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        onIntentConsumed()
    }

    val updatedActiveMediaList = remember(activeMediaList, favoriteUris) {
        activeMediaList.map { it.copy(isFavorite = it.uri.toString() in favoriteUris) }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute != "media_pager" && currentRoute != "shorts_player" && currentRoute != "photo_editor" && currentRoute != "video_editor",
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp)
            ) {
                MenuScreen(
                    currentRoute = currentRoute,
                    onCloseMenu = {
                        scope.launch { drawerState.close() }
                    },
                    onNavigateToRoute = { route ->
                        scope.launch {
                            drawerState.close()
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                )
            }
        }
    ) {
        SharedTransitionLayout {
            NavHost(
                navController = navController,
                startDestination = "home"
            ) {
                composable(
                    route = "home",
                    exitTransition = {
                        fadeOut(tween(300, easing = FastOutSlowInEasing))
                    },
                    popEnterTransition = {
                        fadeIn(tween(300, easing = FastOutSlowInEasing))
                    }
                ) {
                    HomeScreen(
                        mediaItems = mediaItemsWithFavorites,
                        albums = albums,
                        focusedMediaId = focusedMediaId,
                        onItemClick = { item, list ->
                            isExternalViewerMode = false
                            activeMediaList = list
                            focusedMediaId = item.id
                            activeInitialIndex = list.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                            navController.navigate("media_pager")
                        },
                        onVideoShortsClick = { item, list ->
                            isExternalViewerMode = false
                            val videoOnlyList = list.filter { it.isVideo }
                            activeMediaList = videoOnlyList
                            focusedMediaId = item.id
                            activeInitialIndex = videoOnlyList.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                            navController.navigate("shorts_player")
                        },
                        onAlbumClick = { album ->
                            selectedAlbumItem = album
                            navController.navigate("album_detail")
                        },
                        onNavigateToRoute = { route ->
                            navController.navigate(route)
                        },
                        onOpenDrawer = {
                            scope.launch { drawerState.open() }
                        },
                        onAddFavorites = { uris ->
                            favoritesManager.addFavorites(uris)
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable
                    )
                }

                composable(
                    route = "album_detail",
                    exitTransition = {
                        fadeOut(tween(300, easing = FastOutSlowInEasing))
                    },
                    popEnterTransition = {
                        fadeIn(tween(300, easing = FastOutSlowInEasing))
                    }
                ) {
                    val album = selectedAlbumItem
                    val albumMedia = remember(album, mediaItemsWithFavorites) {
                        if (album != null) {
                            mediaItemsWithFavorites.filter {
                                "${it.volumeName}_${it.bucketId}" == album.id || it.bucketId == album.id || (album.id.isBlank() && it.albumName == album.name)
                            }
                        } else emptyList()
                    }

                    HomeScreen(
                        mediaItems = albumMedia,
                        albums = emptyList(),
                        focusedMediaId = focusedMediaId,
                        onItemClick = { item, list ->
                            isExternalViewerMode = false
                            activeMediaList = list
                            focusedMediaId = item.id
                            activeInitialIndex = list.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                            navController.navigate("media_pager")
                        },
                        onVideoShortsClick = { item, list ->
                            isExternalViewerMode = false
                            val videoOnlyList = list.filter { it.isVideo }
                            activeMediaList = videoOnlyList
                            focusedMediaId = item.id
                            activeInitialIndex = videoOnlyList.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                            navController.navigate("shorts_player")
                        },
                        onNavigateToRoute = { route ->
                            navController.navigate(route)
                        },
                        onOpenDrawer = {
                            scope.launch { drawerState.open() }
                        },
                        onAddFavorites = { uris ->
                            favoritesManager.addFavorites(uris)
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable
                    )
                }

                composable(
                    route = "media_pager",
                    enterTransition = {
                        fadeIn(tween(300, easing = FastOutSlowInEasing))
                    },
                    exitTransition = {
                        fadeOut(tween(300, easing = FastOutSlowInEasing))
                    },
                    popEnterTransition = {
                        fadeIn(tween(300, easing = FastOutSlowInEasing))
                    },
                    popExitTransition = {
                        fadeOut(tween(300, easing = FastOutSlowInEasing))
                    }
                ) {
                    MediaPagerScreen(
                        mediaItems = updatedActiveMediaList,
                        initialIndex = activeInitialIndex,
                        onBackClick = {
                            if (isExternalViewerMode) {
                                (context as? android.app.Activity)?.finishAndRemoveTask()
                            } else if (!navController.popBackStack()) {
                                (context as? android.app.Activity)?.finish()
                            }
                        },
                        onCurrentItemChanged = { item ->
                            focusedMediaId = item.id
                        },
                        onToggleFavorite = { mediaItem ->
                            favoritesManager.toggleFavorite(mediaItem.uri.toString())
                        },
                        onDeleteMediaItem = { mediaItem ->
                            val updatedList = activeMediaList.filter { it.id != mediaItem.id }
                            activeMediaList = updatedList
                            if (updatedList.isEmpty()) {
                                navController.popBackStack()
                            }
                        },
                        onEditClick = { mediaItem ->
                            editingMediaItem = mediaItem
                            if (mediaItem.isVideo) {
                                navController.navigate("video_editor")
                            } else {
                                navController.navigate("photo_editor")
                            }
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable
                    )
                }

                composable(
                    route = "shorts_player",
                    enterTransition = {
                        fadeIn(tween(300, easing = FastOutSlowInEasing))
                    },
                    exitTransition = {
                        fadeOut(tween(300, easing = FastOutSlowInEasing))
                    },
                    popEnterTransition = {
                        fadeIn(tween(300, easing = FastOutSlowInEasing))
                    },
                    popExitTransition = {
                        fadeOut(tween(300, easing = FastOutSlowInEasing))
                    }
                ) {
                    ShortsPlayerScreen(
                        mediaItems = updatedActiveMediaList,
                        initialIndex = activeInitialIndex,
                        onBackClick = { navController.popBackStack() },
                        onCurrentItemChanged = { item ->
                            focusedMediaId = item.id
                        },
                        onToggleFavorite = { mediaItem ->
                            favoritesManager.toggleFavorite(mediaItem.uri.toString())
                        },
                        onDeleteMediaItem = { mediaItem ->
                            val updatedList = activeMediaList.filter { it.id != mediaItem.id }
                            activeMediaList = updatedList
                            if (updatedList.isEmpty()) {
                                navController.popBackStack()
                            }
                        },
                        onEditClick = { mediaItem ->
                            editingMediaItem = mediaItem
                            navController.navigate("video_editor")
                        }
                    )
                }

                composable(
                    route = "photo_editor",
                    enterTransition = {
                        fadeIn(tween(300, easing = FastOutSlowInEasing))
                    },
                    exitTransition = {
                        fadeOut(tween(300, easing = FastOutSlowInEasing))
                    },
                    popEnterTransition = {
                        fadeIn(tween(300, easing = FastOutSlowInEasing))
                    },
                    popExitTransition = {
                        fadeOut(tween(300, easing = FastOutSlowInEasing))
                    }
                ) {
                    editingMediaItem?.let { item ->
                        PhotoEditorScreen(
                            mediaItem = item,
                            onNavigateBack = { navController.popBackStack() },
                            onSaveSuccess = { _ ->
                                navController.popBackStack()
                            }
                        )
                    } ?: LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }

                composable(
                    route = "video_editor",
                    enterTransition = {
                        fadeIn(tween(300, easing = FastOutSlowInEasing))
                    },
                    exitTransition = {
                        fadeOut(tween(300, easing = FastOutSlowInEasing))
                    },
                    popEnterTransition = {
                        fadeIn(tween(300, easing = FastOutSlowInEasing))
                    },
                    popExitTransition = {
                        fadeOut(tween(300, easing = FastOutSlowInEasing))
                    }
                ) {
                    editingMediaItem?.let { item ->
                        VideoEditorScreen(
                            mediaItem = item,
                            onNavigateBack = { navController.popBackStack() },
                            onSaveSuccess = { _ ->
                                navController.popBackStack()
                            }
                        )
                    } ?: LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }

                composable("settings") {
                    SettingsPage(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateTo = { route ->
                            when (route) {
                                "general" -> navController.navigate("general_settings")
                                "appearance" -> navController.navigate("appearance")
                                "interface_and_interaction" -> navController.navigate("interface_and_interaction")
                            }
                        }
                    )
                }

                composable("general_settings") {
                    GeneralSettingsPage(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("appearance") {
                    AppearancePreferences(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateTo = { route ->
                            when (route) {
                                "languages" -> navController.navigate("languages")
                                "dark_theme" -> navController.navigate("dark_theme")
                                "dynamic_color" -> navController.navigate("dynamic_color")
                            }
                        }
                    )
                }

                composable("interface_and_interaction") {
                    InterfaceAndInteractionPreferences(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("dark_theme") {
                    DarkThemePreferences(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("dynamic_color") {
                    DynamicColorPreferences(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("languages") {
                    LanguagesPage(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("sponsor") {
                    SponsorsPage(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("troubleshooting") {
                    TroubleShootingPage(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("about") {
                    AboutPage(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToCreditsPage = { navController.navigate("credits") },
                        onNavigateToUpdatePage = { navController.navigate("update") }
                    )
                }

                composable("credits") {
                    CreditsPage(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("update") {
                    UpdatePage(
                        onNavigateBack = { navController.popBackStack() },
                        triggerUpdate = false
                    )
                }

                composable(
                    route = "trash",
                    enterTransition = {
                        fadeIn(tween(300, easing = FastOutSlowInEasing))
                    },
                    exitTransition = {
                        fadeOut(tween(300, easing = FastOutSlowInEasing))
                    },
                    popEnterTransition = {
                        fadeIn(tween(300, easing = FastOutSlowInEasing))
                    },
                    popExitTransition = {
                        fadeOut(tween(300, easing = FastOutSlowInEasing))
                    }
                ) {
                    TrashScreen(
                        repository = repository,
                        onBackClick = { navController.popBackStack() },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable
                    )
                }
            }
        }
    }
}


