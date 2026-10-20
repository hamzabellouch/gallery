package com.tkno.gallery

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.os.ConfigurationCompat
import androidx.core.text.TextUtilsCompat
import androidx.navigation.compose.rememberNavController
import com.tkno.gallery.data.model.Album
import com.tkno.gallery.data.model.MediaItem
import com.tkno.gallery.data.repository.FavoritesManager
import com.tkno.gallery.data.repository.MediaStoreRepository
import com.tkno.gallery.theme.GalleryTheme
import com.tkno.gallery.ui.components.PermissionHandler
import com.tkno.gallery.ui.navigation.NavGraph
import com.tkno.gallery.util.LanguageManager
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var repository: MediaStoreRepository
    private lateinit var favoritesManager: FavoritesManager
    private val incomingIntentFlow = MutableStateFlow<Intent?>(null)

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)
        val langTag = prefs.getString("app_language", "system") ?: "system"
        if (langTag != "system") {
            val localizedContext = LanguageManager.wrapContext(newBase, langTag)
            super.attachBaseContext(localizedContext)
            return
        }
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enable120HzDisplayMode()

        repository = MediaStoreRepository(applicationContext)
        favoritesManager = FavoritesManager(applicationContext)
        incomingIntentFlow.value = intent

        val prefs = getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)

        setContent {
            val composeContext = LocalContext.current
            var appLanguagePref by remember { mutableStateOf(prefs.getString("app_language", "system") ?: "system") }

            DisposableEffect(prefs) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
                    if (key == "app_language") {
                        appLanguagePref = p.getString("app_language", "system") ?: "system"
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    prefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            val localeContext = remember(appLanguagePref, composeContext) {
                LanguageManager.wrapContext(composeContext, appLanguagePref)
            }
            val currentLocale = remember(appLanguagePref) {
                if (appLanguagePref == "system") {
                    ConfigurationCompat.getLocales(android.content.res.Resources.getSystem().configuration).get(0)
                        ?: Locale.getDefault()
                } else {
                    val custom = LanguageManager.loadCustomLanguage(composeContext, appLanguagePref)
                    if (custom != null) {
                        val cleanTag = when {
                            custom.code.startsWith("b+") -> custom.code.removePrefix("b+").replace('+', '-')
                            else -> custom.code
                        }
                        Locale.forLanguageTag(cleanTag)
                    } else {
                        Locale.forLanguageTag(appLanguagePref)
                    }
                }
            }
            val isRtl = remember(currentLocale, appLanguagePref) {
                val custom = LanguageManager.loadCustomLanguage(composeContext, appLanguagePref)
                if (custom != null) {
                    custom.direction.equals("rtl", ignoreCase = true)
                } else {
                    TextUtilsCompat.getLayoutDirectionFromLocale(currentLocale) == android.view.View.LAYOUT_DIRECTION_RTL
                }
            }
            val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(
                LocalContext provides localeContext,
                LocalLayoutDirection provides layoutDirection
            ) {
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
                        val albums = remember(mediaItems) {
                            if (mediaItems.isNotEmpty()) {
                                repository.getAlbumsFromMediaList(mediaItems)
                            } else emptyList()
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
