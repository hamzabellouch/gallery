package com.tkno.gallery.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = TaskbarActivePrimaryDark,
    secondary = PurpleGrey80,
    secondaryContainer = TaskbarIndicatorCapsuleDark,
    onSecondaryContainer = TaskbarActivePrimaryDark,
    tertiary = Pink80,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    surfaceContainer = TaskbarNavContainerDark,
    onSurfaceVariant = TaskbarInactiveVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = TaskbarActivePrimaryLight,
    secondary = PurpleGrey40,
    secondaryContainer = TaskbarIndicatorCapsuleLight,
    onSecondaryContainer = TaskbarActivePrimaryLight,
    tertiary = Pink40,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    surfaceContainer = TaskbarNavContainerLight,
    onSurfaceVariant = TaskbarInactiveVariantLight
)

@Composable
fun GalleryTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val prefs = remember(context) { context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE) }

    var darkThemeMode by remember { mutableIntStateOf(prefs.getInt("dark_theme_mode", 0)) }
    var dynamicColor by remember { mutableStateOf(prefs.getBoolean("dynamic_color", true)) }
    var dynamicColorIcons by remember { mutableStateOf(prefs.getBoolean("dynamic_color_icons", false)) }

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            when (key) {
                "dark_theme_mode" -> darkThemeMode = p.getInt("dark_theme_mode", 0)
                "dynamic_color" -> dynamicColor = p.getBoolean("dynamic_color", true)
                "dynamic_color_icons" -> dynamicColorIcons = p.getBoolean("dynamic_color_icons", false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val darkTheme = when (darkThemeMode) {
        1 -> true
        2 -> false
        else -> systemDark
    }

    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !dynamicColorIcons) {
        baseColorScheme.copy(
            primary = if (darkTheme) TaskbarActivePrimaryDark else TaskbarActivePrimaryLight,
            secondaryContainer = if (darkTheme) TaskbarIndicatorCapsuleDark else TaskbarIndicatorCapsuleLight,
            onSecondaryContainer = if (darkTheme) TaskbarActivePrimaryDark else TaskbarActivePrimaryLight,
            onSurfaceVariant = if (darkTheme) TaskbarInactiveVariantDark else TaskbarInactiveVariantLight,
            surfaceContainer = if (darkTheme) TaskbarNavContainerDark else TaskbarNavContainerLight
        )
    } else {
        baseColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

