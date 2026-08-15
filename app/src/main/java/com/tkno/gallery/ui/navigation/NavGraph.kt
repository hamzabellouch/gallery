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
import com.tkno.gallery.ui.page.settings.about.UpdatePage
import com.tkno.gallery.ui.screens.home.HomeScreen
import com.tkno.gallery.ui.screens.menu.*
import com.tkno.gallery.ui.screens.viewer.MediaPagerScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    mediaItems: List<MediaItem>,
    albums: List<Album>,
    favoritesManager: FavoritesManager
) {
    val favoriteUris by favoritesManager.favoriteUris.collectAsState()

    val mediaItemsWithFavorites = remember(mediaItems, favoriteUris) {
        mediaItems.map { it.copy(isFavorite = it.uri.toString() in favoriteUris) }
    }

    var activeMediaList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var activeInitialIndex by remember { mutableIntStateOf(0) }
    var selectedAlbumItem by remember { mutableStateOf<Album?>(null) }

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
        gesturesEnabled = currentRoute != "media_pager",
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
                        onItemClick = { item, list ->
                            activeMediaList = list
                            activeInitialIndex = list.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                            navController.navigate("media_pager")
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
                        onItemClick = { item, list ->
                            activeMediaList = list
                            activeInitialIndex = list.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                            navController.navigate("media_pager")
                        },
                        onNavigateToRoute = { route ->
                            navController.navigate(route)
                        },
                        onOpenDrawer = {
                            scope.launch { drawerState.open() }
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
                        onBackClick = { navController.popBackStack() },
                        onToggleFavorite = { mediaItem ->
                            favoritesManager.toggleFavorite(mediaItem.uri.toString())
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable
                    )
                }

                composable("settings") {
                    SettingsPage(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateTo = { route ->
                            when (route) {
                                "general" -> navController.navigate("general_settings")
                                "appearance" -> navController.navigate("appearance")
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
            }
        }
    }
}


