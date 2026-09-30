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

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private lateinit var repository: MediaStoreRepository
    private lateinit var favoritesManager: FavoritesManager
    private val incomingIntentFlow = MutableStateFlow<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enable120HzDisplayMode()

        repository = MediaStoreRepository(applicationContext)
        favoritesManager = FavoritesManager(applicationContext)
        incomingIntentFlow.value = intent

        setContent {
            GalleryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val incomingIntent by incomingIntentFlow.collectAsState()
                    val isMediaIntent = remember(incomingIntent) {
                        com.tkno.gallery.util.MediaIntentResolver.isMediaIntent(incomingIntent)
                    }

                    PermissionHandler(bypassPermission = isMediaIntent) {
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
                            repository = repository,
                            incomingIntent = incomingIntent,
                            onIntentConsumed = {
                                incomingIntentFlow.value = null
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingIntentFlow.value = intent
    }

    private fun enable120HzDisplayMode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val activeDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    display
                } else {
                    windowManager?.defaultDisplay
                }
                activeDisplay?.supportedModes?.maxByOrNull { it.refreshRate }?.let { maxMode ->
                    val layoutParams = window?.attributes
                    if (layoutParams != null && layoutParams.preferredDisplayModeId != maxMode.modeId) {
                        layoutParams.preferredDisplayModeId = maxMode.modeId
                        window.attributes = layoutParams
                    }
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}
