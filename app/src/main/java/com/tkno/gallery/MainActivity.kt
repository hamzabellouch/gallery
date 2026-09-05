package com.tkno.gallery

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.tkno.gallery.data.model.Album
import com.tkno.gallery.data.model.MediaItem
import com.tkno.gallery.data.repository.FavoritesManager
import com.tkno.gallery.data.repository.MediaStoreRepository
import com.tkno.gallery.theme.GalleryTheme
import com.tkno.gallery.ui.components.PermissionHandler
import com.tkno.gallery.ui.navigation.NavGraph

class MainActivity : ComponentActivity() {

    private lateinit var repository: MediaStoreRepository
    private lateinit var favoritesManager: FavoritesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enable120HzDisplayMode()

        repository = MediaStoreRepository(applicationContext)
        favoritesManager = FavoritesManager(applicationContext)

        setContent {
            GalleryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionHandler {
                        val mediaItems by repository.getMediaItemsFlow().collectAsState(initial = emptyList())
                        var albums by remember { mutableStateOf<List<Album>>(emptyList()) }

                        LaunchedEffect(mediaItems) {
                            if (mediaItems.isNotEmpty()) {
                                albums = repository.getAlbums()
                            }
                        }

                        val navController = rememberNavController()
                        NavGraph(
                            navController = navController,
                            mediaItems = mediaItems,
                            albums = albums,
                            favoritesManager = favoritesManager,
                            repository = repository
                        )
                    }
                }
            }
        }
    }

    private fun enable120HzDisplayMode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val activeDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    display
                } else {
                    windowManager.defaultDisplay
                }
                activeDisplay?.supportedModes?.maxByOrNull { it.refreshRate }?.let { maxMode ->
                    val layoutParams = window.attributes
                    layoutParams.preferredDisplayModeId = maxMode.modeId
                    window.attributes = layoutParams
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
