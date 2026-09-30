package com.tkno.gallery.ui.screens.menu

import com.tkno.gallery.ui.components.CustomIcons

import android.app.LocaleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ContactSupport
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material3.*
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.tkno.gallery.R
import com.tkno.gallery.ui.component.*

import com.tkno.gallery.ui.page.settings.BasePreferencePage
import com.tkno.gallery.ui.page.settings.about.UpdatePage
import com.tkno.gallery.ui.svg.drawablevectors.DynamicColorImageVectors
import com.tkno.gallery.ui.svg.drawablevectors.coder
import java.util.Locale

enum class MenuSubScreen {
    Main, Settings, GeneralSettings, LookAndFeel, InterfaceAndInteraction, Languages, DarkTheme, Sponsor, Troubleshooting, About, Credits, Update
}

@Composable
fun MenuScreen(
    modifier: Modifier = Modifier,
    currentRoute: String? = null,
    onCloseMenu: () -> Unit = {},
    onNavigateToRoute: (String) -> Unit = {}
) {
    MainMenuList(
        currentRoute = currentRoute,
        onCloseDrawer = onCloseMenu,
        onNavigateToRoute = onNavigateToRoute
    )
}

@Composable
fun MainMenuList(
    currentRoute: String? = null,
    onCloseDrawer: () -> Unit = {},
    onNavigateToRoute: (String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.offset(x = (-12).dp)
                ) {
                    IconButton(onClick = onCloseDrawer) {
                        Icon(
                            imageVector = CustomIcons.LeftPanelClose,
                            contentDescription = stringResource(id = R.string.nav_menu),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.menu),
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            val isSettingsSelected = currentRoute in listOf("settings", "general_settings", "appearance", "dark_theme", "dynamic_color", "languages", "interface_and_interaction")
            val isSponsorSelected = currentRoute == "sponsor"
            val isTroubleshootingSelected = currentRoute == "troubleshooting"
            val isAboutSelected = currentRoute in listOf("about", "credits", "update")

            val drawerItemColors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                NavigationDrawerItem(
                    label = { Text(stringResource(id = R.string.settings)) },
                    icon = {
                        Icon(
                            imageVector = if (isSettingsSelected) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = null
                        )
                    },
                    selected = isSettingsSelected,
                    onClick = { onNavigateToRoute("settings") },
                    colors = drawerItemColors,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(id = R.string.sponsor)) },
                    icon = {
                        Icon(
                            imageVector = if (isSponsorSelected) Icons.Filled.VolunteerActivism else Icons.Outlined.VolunteerActivism,
                            contentDescription = null
                        )
                    },
                    selected = isSponsorSelected,
                    onClick = { onNavigateToRoute("sponsor") },
                    colors = drawerItemColors,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(id = R.string.trouble_shooting)) },
                    icon = {
                        Icon(
                            imageVector = if (isTroubleshootingSelected) Icons.Filled.BugReport else Icons.Outlined.BugReport,
                            contentDescription = null
                        )
                    },
                    selected = isTroubleshootingSelected,
                    onClick = { onNavigateToRoute("troubleshooting") },
                    colors = drawerItemColors,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(id = R.string.about)) },
                    icon = {
                        Icon(
                            imageVector = if (isAboutSelected) Icons.Filled.Info else Icons.Outlined.Info,
                            contentDescription = null
                        )
                    },
                    selected = isAboutSelected,
                    onClick = { onNavigateToRoute("about") },
                    colors = drawerItemColors,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsPage(
    onNavigateBack: () -> Unit,
    onNavigateTo: (String) -> Unit
) {
    BasePreferencePage(
        title = stringResource(id = R.string.settings),
        onBack = onNavigateBack
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues
        ) {
            item {
                PreferenceItem(
                    title = stringResource(id = R.string.general_settings),
                    description = stringResource(id = R.string.general_settings_desc),
                    icon = Icons.Rounded.SettingsApplications,
                    onClick = { onNavigateTo("general") }
                )
            }
            item {
                PreferenceItem(
                    title = stringResource(id = R.string.look_and_feel),
                    description = stringResource(id = R.string.display_settings),
                    icon = Icons.Rounded.Palette,
                    onClick = { onNavigateTo("appearance") }
                )
            }
            item {
                PreferenceItem(
                    title = stringResource(R.string.interface_and_interaction),
                    description = stringResource(R.string.interface_and_interaction_desc),
                    icon = Icons.Outlined.TouchApp,
                    onClick = { onNavigateTo("interface_and_interaction") }
                )
            }
        }
    }
}

@Composable
fun GeneralSettingsPage(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gallery_prefs", android.content.Context.MODE_PRIVATE) }
    var showResolutionBadges by remember {
        mutableStateOf(prefs.getBoolean("show_resolution_badges", true))
    }
    var cardRoundedCorners by remember {
        mutableStateOf(prefs.getBoolean("card_rounded_corners", true))
    }

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == "show_resolution_badges") {
                showResolutionBadges = p.getBoolean("show_resolution_badges", true)
            } else if (key == "card_rounded_corners") {
                cardRoundedCorners = p.getBoolean("card_rounded_corners", true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    BasePreferencePage(
        title = stringResource(id = R.string.general_settings),
        onBack = onNavigateBack
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues
        ) {
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.show_resolution_badges),
                    icon = Icons.Outlined.HighQuality,
                    isChecked = showResolutionBadges,
                    description = stringResource(id = R.string.show_resolution_badges_desc),
                    onClick = {
                        val newValue = !showResolutionBadges
                        showResolutionBadges = newValue
                        prefs.edit().putBoolean("show_resolution_badges", newValue).apply()
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = stringResource(id = R.string.card_rounded_corners),
                    icon = Icons.Outlined.RoundedCorner,
                    isChecked = cardRoundedCorners,
                    description = stringResource(id = R.string.card_rounded_corners_desc),
                    onClick = {
                        val newValue = !cardRoundedCorners
                        cardRoundedCorners = newValue
                        prefs.edit().putBoolean("card_rounded_corners", newValue).apply()
                    }
                )
            }
        }
    }
}

fun getSavedLocaleDisplayName(context: android.content.Context): String {
    val prefs = context.getSharedPreferences("gallery_prefs", android.content.Context.MODE_PRIVATE)
    val langTag = prefs.getString("app_language", "system") ?: "system"
    if (langTag == "system") return context.getString(R.string.follow_system)
    return when (langTag) {
        "en" -> "English"
        "ar" -> "العربية"
        "az" -> "Azərbaycan"
        "be" -> "Беларуская"
        "zh-Hans" -> "简体中文"
        "zh-Hant" -> "繁體中文"
        "hr" -> "Hrvatski"
        "cs" -> "Čeština"
        "da" -> "Dansk"
        "nl" -> "Nederlands"
        "fil" -> "Filipino"
        "fr" -> "Français"
        "de" -> "Deutsch"
        "el" -> "Ελληνικά"
        "hi" -> "हिन्दी"
        "hu" -> "Magyar"
        "in" -> "Bahasa Indonesia"
        "it" -> "Italiano"
        "ja" -> "日本語"
        "ko" -> "한국어"
        "ms" -> "Bahasa Melayu"
        "mn" -> "Монгол"
        "fa" -> "فارسی"
        "pl" -> "Polski"
        "pt" -> "Português"
        "ru" -> "Русский"
        "sr" -> "Српски"
        "si" -> "සිංහල"
        "es" -> "Español"
        "sv" -> "Svenska"
        "th" -> "ไทย"
        "tr" -> "Türkçe"
        "uk" -> "Українська"
        "vi" -> "Tiếng Việt"
        "b+zgh", "zgh" -> "ⵜⴰⵎⴰⵣⵉⵖⵜ"
        else -> Locale.forLanguageTag(langTag).getDisplayName(Locale.forLanguageTag(langTag)).replaceFirstChar { it.uppercase() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearancePreferences(
    onNavigateBack: () -> Unit,
    onNavigateTo: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gallery_prefs", android.content.Context.MODE_PRIVATE) }

    var darkThemePref by remember { mutableIntStateOf(prefs.getInt("dark_theme_mode", 0)) }
    var isDynamicColor by remember { mutableStateOf(prefs.getBoolean("dynamic_color", true)) }
    var dynamicColorIcons by remember { mutableStateOf(prefs.getBoolean("dynamic_color_icons", false)) }

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == "dark_theme_mode") {
                darkThemePref = p.getInt("dark_theme_mode", 0)
            } else if (key == "dynamic_color") {
                isDynamicColor = p.getBoolean("dynamic_color", true)
            } else if (key == "dynamic_color_icons") {
                dynamicColorIcons = p.getBoolean("dynamic_color_icons", false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val isDark = when (darkThemePref) {
        1 -> true
        2 -> false
        else -> isSystemInDarkTheme()
    }

    val darkThemeDesc = when (darkThemePref) {
        1 -> stringResource(id = R.string.on)
        2 -> stringResource(id = R.string.off)
        else -> stringResource(id = R.string.follow_system)
    }

    val dynamicColorDesc = when {
        !isDynamicColor -> stringResource(id = R.string.off)
        dynamicColorIcons -> stringResource(id = R.string.icon_color_dynamic)
        else -> stringResource(id = R.string.icon_color_original)
    }

    BasePreferencePage(
        title = stringResource(id = R.string.look_and_feel),
        onBack = onNavigateBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PreferenceSwitchWithDivider(
                    title = stringResource(id = R.string.dynamic_color),
                    icon = Icons.Outlined.Colorize,
                    isChecked = isDynamicColor,
                    description = dynamicColorDesc,
                    onChecked = {
                        val newValue = !isDynamicColor
                        isDynamicColor = newValue
                        prefs.edit().putBoolean("dynamic_color", newValue).apply()
                    },
                    onClick = { onNavigateTo("dynamic_color") },
                )
            }
            PreferenceSwitchWithDivider(
                title = stringResource(id = R.string.dark_theme),
                icon = if (isDark) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                isChecked = isDark,
                description = darkThemeDesc,
                onChecked = {
                    val newPref = if (isDark) 2 else 1
                    darkThemePref = newPref
                    prefs.edit().putInt("dark_theme_mode", newPref).apply()
                },
                onClick = { onNavigateTo("dark_theme") },
            )
            PreferenceItem(
                title = stringResource(R.string.language),
                icon = Icons.Outlined.Language,
                description = getSavedLocaleDisplayName(context),
            ) {
                onNavigateTo("languages")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterfaceAndInteractionPreferences(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gallery_prefs", android.content.Context.MODE_PRIVATE) }
    var useClassicViewerBar by remember { mutableStateOf(prefs.getBoolean("classic_viewer_bar", false)) }
    var useClassicTaskbar by remember { mutableStateOf(prefs.getBoolean("use_classic_taskbar", false)) }

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == "classic_viewer_bar") {
                useClassicViewerBar = p.getBoolean("classic_viewer_bar", false)
            } else if (key == "use_classic_taskbar") {
                useClassicTaskbar = p.getBoolean("use_classic_taskbar", false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    BasePreferencePage(
        title = stringResource(id = R.string.interface_and_interaction),
        onBack = onNavigateBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            PreferenceSwitch(
                title = stringResource(id = R.string.use_classic_taskbar),
                icon = Icons.Outlined.Dock,
                isChecked = useClassicTaskbar,
                description = stringResource(id = R.string.use_classic_taskbar_desc),
                onClick = {
                    val newValue = !useClassicTaskbar
                    useClassicTaskbar = newValue
                    prefs.edit().putBoolean("use_classic_taskbar", newValue).apply()
                }
            )
            PreferenceSwitch(
                title = stringResource(id = R.string.classic_viewer_bar),
                icon = Icons.Outlined.ViewAgenda,
                isChecked = useClassicViewerBar,
                description = stringResource(id = R.string.classic_viewer_bar_desc),
                onClick = {
                    val newValue = !useClassicViewerBar
                    useClassicViewerBar = newValue
                    prefs.edit().putBoolean("classic_viewer_bar", newValue).apply()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkThemePreferences(onNavigateBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gallery_prefs", android.content.Context.MODE_PRIVATE) }
    var darkThemePref by remember { mutableIntStateOf(prefs.getInt("dark_theme_mode", 0)) }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val typography = MaterialTheme.typography
            val overrideTypography = remember(typography) {
                typography.copy(headlineMedium = typography.displaySmall)
            }
            MaterialTheme(typography = overrideTypography) {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.dark_theme),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    navigationIcon = { BackButton { onNavigateBack() } },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        content = { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 48.dp
                )
            ) {
                if (Build.VERSION.SDK_INT >= 29) {
                    item {
                        PreferenceSingleChoiceItem(
                            text = stringResource(R.string.follow_system),
                            selected = darkThemePref == 0,
                            onClick = {
                                darkThemePref = 0
                                prefs.edit().putInt("dark_theme_mode", 0).apply()
                            }
                        )
                    }
                }
                item {
                    PreferenceSingleChoiceItem(
                        text = stringResource(R.string.on),
                        selected = darkThemePref == 1,
                        onClick = {
                            darkThemePref = 1
                            prefs.edit().putInt("dark_theme_mode", 1).apply()
                        }
                    )
                }
                item {
                    PreferenceSingleChoiceItem(
                        text = stringResource(R.string.off),
                        selected = darkThemePref == 2,
                        onClick = {
                            darkThemePref = 2
                            prefs.edit().putInt("dark_theme_mode", 2).apply()
                        }
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicColorPreferences(onNavigateBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gallery_prefs", android.content.Context.MODE_PRIVATE) }
    var isDynamicColor by remember { mutableStateOf(prefs.getBoolean("dynamic_color", true)) }
    var dynamicColorIcons by remember { mutableStateOf(prefs.getBoolean("dynamic_color_icons", false)) }

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == "dynamic_color") {
                isDynamicColor = p.getBoolean("dynamic_color", true)
            } else if (key == "dynamic_color_icons") {
                dynamicColorIcons = p.getBoolean("dynamic_color_icons", false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val typography = MaterialTheme.typography
            val overrideTypography = remember(typography) {
                typography.copy(headlineMedium = typography.displaySmall)
            }
            MaterialTheme(typography = overrideTypography) {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.dynamic_color),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    navigationIcon = { BackButton { onNavigateBack() } },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        content = { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 48.dp
                )
            ) {
                item {
                    PreferenceSwitch(
                        title = stringResource(id = R.string.dynamic_color),
                        description = stringResource(id = R.string.dynamic_color_desc),
                        icon = Icons.Outlined.Colorize,
                        isChecked = isDynamicColor,
                        onClick = {
                            val newValue = !isDynamicColor
                            isDynamicColor = newValue
                            prefs.edit().putBoolean("dynamic_color", newValue).apply()
                        }
                    )
                }

                if (isDynamicColor) {
                    item {
                        PreferenceSubtitle(text = stringResource(R.string.dynamic_color_icons))
                    }
                    item {
                        PreferenceSingleChoiceItem(
                            text = stringResource(R.string.icon_color_original),
                            selected = !dynamicColorIcons,
                            onClick = {
                                dynamicColorIcons = false
                                prefs.edit().putBoolean("dynamic_color_icons", false).apply()
                            }
                        )
                    }
                    item {
                        PreferenceSingleChoiceItem(
                            text = stringResource(R.string.icon_color_dynamic),
                            selected = dynamicColorIcons,
                            onClick = {
                                dynamicColorIcons = true
                                prefs.edit().putBoolean("dynamic_color_icons", true).apply()
                            }
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagesPage(onNavigateBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gallery_prefs", android.content.Context.MODE_PRIVATE) }
    var selectedLangTag by remember { mutableStateOf(prefs.getString("app_language", "system") ?: "system") }

    val suggestedLanguages = remember {
        listOf(
            Triple("English", "en", Locale.ENGLISH),
            Triple("العربية", "ar", Locale.forLanguageTag("ar")),
        )
    }

    val allLanguagesList = remember {
        listOf(
            Triple("العربية", "ar", Locale.forLanguageTag("ar")),
            Triple("Azərbaycan", "az", Locale.forLanguageTag("az")),
            Triple("Беларуская", "be", Locale.forLanguageTag("be")),
            Triple("简体中文", "zh-Hans", Locale.forLanguageTag("zh-Hans")),
            Triple("繁體中文", "zh-Hant", Locale.forLanguageTag("zh-Hant")),
            Triple("Hrvatski", "hr", Locale.forLanguageTag("hr")),
            Triple("Čeština", "cs", Locale.forLanguageTag("cs")),
            Triple("Dansk", "da", Locale.forLanguageTag("da")),
            Triple("Nederlands", "nl", Locale.forLanguageTag("nl")),
            Triple("English", "en", Locale.ENGLISH),
            Triple("Filipino", "fil", Locale.forLanguageTag("fil")),
            Triple("Français", "fr", Locale.FRENCH),
            Triple("Deutsch", "de", Locale.GERMAN),
            Triple("Ελληνικά", "el", Locale.forLanguageTag("el")),
            Triple("हिन्दी", "hi", Locale.forLanguageTag("hi")),
            Triple("Magyar", "hu", Locale.forLanguageTag("hu")),
            Triple("Bahasa Indonesia", "in", Locale.forLanguageTag("in")),
            Triple("Italiano", "it", Locale.ITALIAN),
            Triple("日本語", "ja", Locale.JAPANESE),
            Triple("한국어", "ko", Locale.KOREAN),
            Triple("Bahasa Melayu", "ms", Locale.forLanguageTag("ms")),
            Triple("Монгол", "mn", Locale.forLanguageTag("mn")),
            Triple("فارسی", "fa", Locale.forLanguageTag("fa")),
            Triple("Polski", "pl", Locale.forLanguageTag("pl")),
            Triple("Português", "pt", Locale.forLanguageTag("pt")),
            Triple("Русский", "ru", Locale.forLanguageTag("ru")),
            Triple("Српски", "sr", Locale.forLanguageTag("sr")),
            Triple("සිංහල", "si", Locale.forLanguageTag("si")),
            Triple("Español", "es", Locale.forLanguageTag("es")),
            Triple("Svenska", "sv", Locale.forLanguageTag("sv")),
            Triple("ไทย", "th", Locale.forLanguageTag("th")),
            Triple("Türkçe", "tr", Locale.forLanguageTag("tr")),
            Triple("Українська", "uk", Locale.forLanguageTag("uk")),
            Triple("Tiếng Việt", "vi", Locale.forLanguageTag("vi")),
            Triple("ⵜⴰⵎⴰⵣⵉⵖⵜ", "zgh", Locale.forLanguageTag("zgh"))
        )
    }

    val deviceLocale = remember {
        ConfigurationCompat.getLocales(context.resources.configuration).get(0) ?: Locale.getDefault()
    }

    val isSystemLangSupported = remember(deviceLocale, allLanguagesList) {
        val devTag = deviceLocale.toLanguageTag().lowercase()
        val devLang = deviceLocale.language.lowercase()
        allLanguagesList.any { (_, tag, _) ->
            val t = tag.lowercase()
            t == devTag || t == devLang || devTag.startsWith(t) || devLang == t.split("-")[0]
        }
    }

    fun setAppLanguage(langTag: String, locale: Locale?) {
        selectedLangTag = langTag
        prefs.edit().putString("app_language", langTag).apply()

        val localeListCompat = if (langTag == "system" || locale == null) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(langTag)
        }
        AppCompatDelegate.setApplicationLocales(localeListCompat)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(android.content.Context.LOCALE_SERVICE) as? LocaleManager
            if (localeManager != null) {
                val localeList = if (langTag == "system" || locale == null) {
                    LocaleList.getEmptyLocaleList()
                } else {
                    LocaleList(locale)
                }
                localeManager.applicationLocales = localeList
            }
        }

        val targetLocale = if (langTag == "system" || locale == null) Locale.getDefault() else locale
        Locale.setDefault(targetLocale)
        val config = context.resources.configuration
        config.setLocale(targetLocale)
        config.setLayoutDirection(targetLocale)
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val typography = MaterialTheme.typography
            val overrideTypography = remember(typography) {
                typography.copy(headlineMedium = typography.displaySmall)
            }
            MaterialTheme(typography = overrideTypography) {
                LargeTopAppBar(
                    title = {
                        Text(text = stringResource(id = R.string.language), color = MaterialTheme.colorScheme.onBackground)
                    },
                    navigationIcon = { BackButton { onNavigateBack() } },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        content = { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 16.dp
                )
            ) {
                item {
                    PreferencesHintCard(
                        title = stringResource(id = R.string.translate),
                        description = stringResource(id = R.string.translate_desc),
                        icon = Icons.Outlined.Translate,
                    ) {
                        uriHandler.openUri("https://github.com/hamzabellouch/gallery")
                    }
                }

                item {
                    PreferenceSubtitle(text = stringResource(id = R.string.suggested))
                }

                item {
                    PreferenceSingleChoiceItem(
                        text = stringResource(id = R.string.follow_system),
                        selected = selectedLangTag == "system",
                        selectedColor = if (!isSystemLangSupported) androidx.compose.ui.graphics.Color(0xFFE53935) else null,
                        onClick = { setAppLanguage("system", null) },
                    )
                }

                items(suggestedLanguages) { (displayName, langTag, locale) ->
                    PreferenceSingleChoiceItem(
                        text = displayName,
                        selected = selectedLangTag == langTag,
                        onClick = { setAppLanguage(langTag, locale) },
                    )
                }

                item {
                    PreferenceSubtitle(text = stringResource(id = R.string.all_languages))
                }

                items(allLanguagesList) { (displayName, langTag, locale) ->
                    PreferenceSingleChoiceItem(
                        text = displayName,
                        selected = selectedLangTag == langTag,
                        onClick = { setAppLanguage(langTag, locale) },
                    )
                }
            }
        },
    )
}


@Composable
fun Conversation(modifier: Modifier = Modifier, text: String) {
    Row(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

/* ---------------- SponsorsPage ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SponsorsPage(onNavigateBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
        canScroll = { true },
    )
    val uriHandler = LocalUriHandler.current

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val typography = MaterialTheme.typography
            val overrideTypography = remember(typography) {
                typography.copy(headlineMedium = typography.displaySmall)
            }

            MaterialTheme(typography = overrideTypography) {
                LargeTopAppBar(
                    title = {
                        Text(text = stringResource(id = R.string.sponsors), color = MaterialTheme.colorScheme.onBackground)
                    },
                    navigationIcon = { BackButton { onNavigateBack() } },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        content = { values ->
            LazyVerticalGrid(
                modifier = Modifier.padding(horizontal = 12.dp),
                columns = GridCells.Fixed(12),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = values,
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Surface(
                        shape = CardDefaults.shape,
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                            Text(
                                modifier = Modifier
                                    .padding(bottom = 4.dp)
                                    .align(Alignment.CenterHorizontally),
                                text = stringResource(id = R.string.msg_from_developer),
                                style = MaterialTheme.typography.labelLarge,
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.developer_avatar),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .aspectRatio(1f, true)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Conversation(
                                        modifier = Modifier.padding(bottom = 12.dp),
                                        text = stringResource(id = R.string.sponsor_msg),
                                    )
                                    Conversation(
                                        modifier = Modifier,
                                        text = stringResource(id = R.string.sponsor_msg2),
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    uriHandler.openUri("https://github.com/sponsors/hamzabellouch")
                                },
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(ButtonDefaults.IconSize),
                                    imageVector = Icons.Outlined.VolunteerActivism,
                                    contentDescription = null,
                                )

                                Text(text = stringResource(id = R.string.sponsor))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    uriHandler.openUri("https://github.com/hamzabellouch/gallery")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFC107),
                                    contentColor = Color(0xFF1E1E1E),
                                ),
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(ButtonDefaults.IconSize),
                                    imageVector = CustomIcons.Star,
                                    contentDescription = null,
                                )

                                Text(text = stringResource(id = R.string.star))
                            }
                        }
                    }
                }
            }
        },
    )
}

/* ---------------- TroubleShootingPage ---------------- */

private const val reportProblemFormUrl = "https://docs.google.com/forms/d/e/1FAIpQLScLCRgTEY0IqBHoJdyzaCDa92a6tOyeOSz1TScqj0e8iQ89MQ/viewform?usp=header"

@Composable
fun TroubleShootingPage(onNavigateBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    var showContactDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences("gallery_prefs", android.content.Context.MODE_PRIVATE) }
    var darkThemePref by remember { mutableIntStateOf(prefs.getInt("dark_theme_mode", 0)) }

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == "dark_theme_mode") {
                darkThemePref = p.getInt("dark_theme_mode", 0)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val isDark = when (darkThemePref) {
        1 -> true
        2 -> false
        else -> isSystemInDarkTheme()
    }

    val emailContainer = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val emailContent = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    val whatsappContainer = if (isDark) Color(0xFF0A2B1D) else Color(0xFFE8F8F0)
    val whatsappContent = if (isDark) Color(0xFF25D366) else Color(0xFF128C7E)

    val facebookContainer = if (isDark) Color(0xFF0D2646) else Color(0xFFE7F3FF)
    val facebookContent = if (isDark) Color(0xFF4599FF) else Color(0xFF1877F2)

    val instagramContainer = if (isDark) Color(0xFF3D1625) else Color(0xFFFDF0F3)
    val instagramContent = if (isDark) Color(0xFFFF527B) else Color(0xFFD82E62)

    val linkedinContainer = if (isDark) Color(0xFF0E2E4E) else Color(0xFFE8F2FF)
    val linkedinContent = if (isDark) Color(0xFF55A4FC) else Color(0xFF0A66C2)

    val xContainer = if (isDark) Color(0xFF16181C) else Color(0xFFF5F8FA)
    val xContent = if (isDark) Color(0xFFE7E9EA) else Color(0xFF0F1419)

    val youtubeContainer = if (isDark) Color(0xFF3A1115) else Color(0xFFFFEBEE)
    val youtubeContent = if (isDark) Color(0xFFE53935) else Color(0xFFCC0000)

    val tiktokContainer = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF1F1F1)
    val tiktokContent = if (isDark) Color(0xFFFFFFFF) else Color(0xFF010101)

    val redditContainer = if (isDark) Color(0xFF3D1E16) else Color(0xFFFFEBE5)
    val redditContent = if (isDark) Color(0xFFFF5A1F) else Color(0xFFFF4500)

    val blueskyContainer = if (isDark) Color(0xFF0A2E4C) else Color(0xFFE8F8FF)
    val blueskyContent = if (isDark) Color(0xFF3BA1FF) else Color(0xFF0085FF)

    val telegramContainer = if (isDark) Color(0xFF0F2C3D) else Color(0xFFE8F5FA)
    val telegramContent = if (isDark) Color(0xFF52B6E9) else Color(0xFF24A1DE)

    BasePreferencePage(
        title = stringResource(R.string.trouble_shooting),
        onBack = onNavigateBack,
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                val pagerState = rememberPagerState(initialPage = 0) { 11 }
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth(),
                    ) { page ->
                        when (page) {
                            0 -> PreferencesHintCard(
                                title = stringResource(R.string.contact),
                                description = stringResource(R.string.contact_desc),
                                icon = Icons.Outlined.Email,
                                containerColor = emailContainer,
                                contentColor = emailContent,
                                textColor = Color.White,
                            ) { showContactDialog = true }

                            1 -> PreferencesHintCard(
                                title = stringResource(id = R.string.whatsapp),
                                icon = painterResource(id = R.drawable.ic_whatsapp),
                                description = stringResource(id = R.string.whatsapp_desc),
                                containerColor = whatsappContainer,
                                contentColor = whatsappContent,
                                textColor = Color.White,
                            ) { 
                                uriHandler.openUri("https://whatsapp.com/channel/0029Vb7MArw0LKZMpjjqOk2P") 
                            }

                            2 -> PreferencesHintCard(
                                title = stringResource(id = R.string.facebook),
                                icon = painterResource(id = R.drawable.ic_facebook),
                                description = stringResource(id = R.string.facebook_desc),
                                containerColor = facebookContainer,
                                contentColor = facebookContent,
                                textColor = Color.White,
                            ) { uriHandler.openUri("https://www.facebook.com/hamzabellouch1") }

                            3 -> PreferencesHintCard(
                                title = stringResource(id = R.string.instagram),
                                icon = painterResource(id = R.drawable.ic_instagram),
                                description = stringResource(id = R.string.instagram_desc),
                                containerColor = instagramContainer,
                                contentColor = instagramContent,
                                textColor = Color.White,
                            ) { uriHandler.openUri("https://www.instagram.com/hamzabellouch0") }

                            4 -> PreferencesHintCard(
                                title = stringResource(id = R.string.linkedin),
                                icon = painterResource(id = R.drawable.ic_linkedin),
                                description = stringResource(id = R.string.linkedin_desc),
                                containerColor = linkedinContainer,
                                contentColor = linkedinContent,
                                textColor = Color.White,
                            ) { uriHandler.openUri("https://www.linkedin.com/in/hamzabellouch") }

                            5 -> PreferencesHintCard(
                                title = stringResource(id = R.string.x_platform),
                                icon = painterResource(id = R.drawable.ic_x),
                                description = stringResource(id = R.string.x_desc),
                                containerColor = xContainer,
                                contentColor = xContent,
                                textColor = Color.White,
                            ) { uriHandler.openUri("https://x.com/hamzabellouch0") }

                            6 -> PreferencesHintCard(
                                title = stringResource(id = R.string.youtube),
                                icon = painterResource(id = R.drawable.ic_youtube),
                                description = stringResource(id = R.string.youtube_desc),
                                containerColor = youtubeContainer,
                                contentColor = youtubeContent,
                                textColor = Color.White,
                            ) { uriHandler.openUri("https://www.youtube.com/@hamzabellouch") }

                            7 -> PreferencesHintCard(
                                title = stringResource(id = R.string.tiktok),
                                icon = painterResource(id = R.drawable.ic_tiktok),
                                description = stringResource(id = R.string.tiktok_desc),
                                containerColor = tiktokContainer,
                                contentColor = tiktokContent,
                                textColor = Color.White,
                            ) { uriHandler.openUri("https://www.tiktok.com/@hamzabellouch0") }

                            8 -> PreferencesHintCard(
                                title = stringResource(id = R.string.reddit),
                                icon = painterResource(id = R.drawable.ic_reddit),
                                description = stringResource(id = R.string.reddit_desc),
                                containerColor = redditContainer,
                                contentColor = redditContent,
                                textColor = Color.White,
                            ) { uriHandler.openUri("https://www.reddit.com") }

                            9 -> PreferencesHintCard(
                                title = stringResource(id = R.string.bluesky),
                                icon = painterResource(id = R.drawable.ic_bluesky),
                                description = stringResource(id = R.string.bluesky_desc),
                                containerColor = blueskyContainer,
                                contentColor = blueskyContent,
                                textColor = Color.White,
                            ) { uriHandler.openUri("https://bsky.app/profile/hamzabellouch.bsky.social") }

                            10 -> PreferencesHintCard(
                                title = stringResource(id = R.string.telegram_channel),
                                icon = painterResource(id = R.drawable.icons8_telegram_app),
                                description = stringResource(id = R.string.telegram_channel_desc),
                                containerColor = telegramContainer,
                                contentColor = telegramContent,
                                textColor = Color.White,
                            ) { uriHandler.openUri("https://t.me/hamzabellouch") }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(11) { pageIndex ->
                            val isSelected = pagerState.currentPage == pageIndex
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }
                }
            }
            item {
                OutlinedCard(modifier = Modifier.padding(16.dp)) {
                    PreferenceInfo(
                        modifier = Modifier,
                        text = stringResource(R.string.issue_tracker_hint),
                    )
                    PreferenceItem(
                        title = stringResource(R.string.links_issue_tracker),
                        description = null,
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        onClick = { uriHandler.openUri("https://github.com/hamzabellouch/gallery/issues") },
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp, bottom = 14.dp, top = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(
                                    role = Role.Button,
                                    onClick = { showReportDialog = true }
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .height(36.dp)
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = CustomIcons.Report,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.problem_report),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showContactDialog) {
        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            confirmButton = {
                FilledButtonWithIcon(
                    icon = Icons.AutoMirrored.Outlined.ArrowForward,
                    text = stringResource(id = R.string.proceed),
                    onClick = {
                        showContactDialog = false
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:hamzabellouchcontact@gmail.com")
                            setPackage("com.google.android.gm")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val fallbackIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:hamzabellouchcontact@gmail.com")
                            }
                            try {
                                context.startActivity(Intent.createChooser(fallbackIntent, "Send Email"))
                            } catch (ex: Exception) {
                                android.widget.Toast.makeText(context, "No email app found", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                )
            },
            dismissButton = {
                OutlinedButtonWithIcon(
                    icon = Icons.Outlined.Cancel,
                    text = stringResource(id = R.string.cancel),
                    onClick = { showContactDialog = false },
                )
            },
            title = { Text(text = stringResource(R.string.contact_developer)) },
            text = { Text(text = stringResource(R.string.contact_developer_confirm)) },
        )
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            icon = {
                Icon(
                    imageVector = CustomIcons.Report,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.problem_report),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.problem_report_dialog_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(
                                role = Role.Button,
                                onClick = {
                                    showReportDialog = false
                                    uriHandler.openUri(reportProblemFormUrl)
                                }
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = CustomIcons.Report,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.open_report_form),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButtonWithIcon(
                    icon = Icons.Outlined.Cancel,
                    text = stringResource(id = R.string.cancel),
                    onClick = { showReportDialog = false },
                )
            }
        )
    }
}

/* ---------------- AboutPage ---------------- */

private const val releaseURL = "https://github.com/hamzabellouch/gallery/releases"
private const val repoUrl = "https://github.com/hamzabellouch/gallery/blob/main/README.md"
private const val githubIssueUrl = "https://github.com/hamzabellouch/gallery/issues"
private const val matrixSpaceUrl = "https://sites.google.com/view/hamzabellouch"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutPage(
    onNavigateBack: () -> Unit,
    onNavigateToCreditsPage: () -> Unit,
    onNavigateToUpdatePage: () -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    val prefs = remember { context.getSharedPreferences("gallery_prefs", android.content.Context.MODE_PRIVATE) }
    var isAutoUpdateEnabled by remember {
        mutableStateOf(prefs.getBoolean("auto_update_enabled", false))
    }

    var showWhatsNewDialog by remember { mutableStateOf(false) }

    // فحص ما إذا كان الزر مخفياً مؤقتاً (لم تمر 24 ساعة بعد)
    val whatsNewDismissedUntil = remember {
        prefs.getLong("whats_new_dismissed_until", 0L)
    }
    var isWhatsNewDismissed by remember {
        mutableStateOf(System.currentTimeMillis() < whatsNewDismissedUntil)
    }

    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }
    val info = "App version: $versionName\nPackage name: ${context.packageName}\nDevice: Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})"
    val uriHandler = LocalUriHandler.current

    fun openUrl(url: String) {
        uriHandler.openUri(url)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val typography = MaterialTheme.typography
            val overrideTypography =
                remember(typography) { typography.copy(headlineMedium = typography.displaySmall) }

            MaterialTheme(typography = overrideTypography) {
                LargeTopAppBar(
                    title = {
                        Text(modifier = Modifier, text = stringResource(id = R.string.about), color = MaterialTheme.colorScheme.onBackground)
                    },
                    navigationIcon = { BackButton { onNavigateBack() } },
                    actions = {
                        if (!isWhatsNewDismissed) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clip(CircleShape)
                            ) {
                                Row(
                                    modifier = Modifier.height(36.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // الجزء الأيسر: الأيقونة + النص (عند الضغط يفتح النافذة)
                                    Row(
                                        modifier = Modifier
                                            .clickable(
                                                role = Role.Button,
                                                onClick = { onNavigateToUpdatePage() }
                                            )
                                            .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = CustomIcons.LocalFireDepartment,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = stringResource(R.string.whats_new),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }

                                    // الجزء الأيمن: زر 'X' لإخفاء الكبسولة لمدة 24 ساعة
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .clickable(
                                                role = Role.Button,
                                                onClick = {
                                                    val dismissUntil = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
                                                    prefs.edit().putLong("whats_new_dismissed_until", dismissUntil).apply()
                                                    isWhatsNewDismissed = true
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.close),
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        content = {
            LazyColumn(modifier = Modifier.padding(it)) {
                item {
                    PreferenceItem(
                        title = stringResource(R.string.readme),
                        description = stringResource(R.string.readme_desc),
                        icon = Icons.Outlined.Description,
                    ) {
                        openUrl(repoUrl)
                    }
                }
                item {
                    PreferenceItem(
                        title = stringResource(R.string.release),
                        description = stringResource(R.string.release_desc),
                        icon = Icons.Outlined.NewReleases,
                    ) {
                        openUrl(releaseURL)
                    }
                }
                item {
                    PreferenceItem(
                        title = stringResource(R.string.github_issue),
                        description = stringResource(R.string.github_issue_desc),
                        icon = Icons.AutoMirrored.Outlined.ContactSupport,
                    ) {
                        openUrl(githubIssueUrl)
                    }
                }
                item {
                    PreferenceItem(
                        title = stringResource(R.string.github_stars),
                        description = stringResource(R.string.github_stars_desc),
                        icon = Icons.Outlined.StarBorder,
                    ) {
                        openUrl("https://github.com/hamzabellouch/gallery")
                    }
                }
                item {
                    PreferenceItem(
                        title = stringResource(R.string.website),
                        description = matrixSpaceUrl,
                        icon = Icons.Outlined.Language,
                    ) {
                        openUrl(matrixSpaceUrl)
                    }
                }
                item {
                    PreferenceItem(
                        title = stringResource(id = R.string.credits),
                        description = stringResource(id = R.string.credits_desc),
                        icon = Icons.Outlined.AutoAwesome,
                    ) {
                        onNavigateToCreditsPage()
                    }
                }
                item {
                    PreferenceSwitchWithDivider(
                        title = stringResource(R.string.auto_update),
                        description = stringResource(R.string.check_for_updates_desc),
                        icon =
                            if (isAutoUpdateEnabled) Icons.Outlined.Update
                            else Icons.Outlined.UpdateDisabled,
                        isChecked = isAutoUpdateEnabled,
                        isSwitchEnabled = true,
                        onClick = onNavigateToUpdatePage,
                        onChecked = {
                            isAutoUpdateEnabled = !isAutoUpdateEnabled
                            prefs.edit().putBoolean("auto_update_enabled", isAutoUpdateEnabled).apply()
                        },
                    )
                }
                item {
                    PreferenceItem(
                        title = stringResource(R.string.privacy_policy),
                        description = stringResource(R.string.privacy_policy_desc),
                        icon = CustomIcons.Policy,
                    ) {
                        openUrl("https://github.com/hamzabellouch/gallery/blob/main/PRIVACY_POLICY.md")
                    }
                }
                item {
                    PreferenceItem(
                        title = stringResource(R.string.version),
                        description = versionName,
                        icon = Icons.Outlined.Info,
                    ) {
                        coroutineScope.launch {
                            clipboard.setClipEntry(ClipEntry(android.content.ClipData.newPlainText("info", info)))
                        }
                        android.widget.Toast.makeText(context, context.getString(R.string.info_copied), android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                item {
                    PreferenceItem(
                        title = stringResource(R.string.package_name),
                        description = context.packageName,
                        icon = Icons.Outlined.Code,
                    ) {
                        coroutineScope.launch {
                            clipboard.setClipEntry(ClipEntry(android.content.ClipData.newPlainText("package_name", context.packageName)))
                        }
                        android.widget.Toast.makeText(context, context.getString(R.string.info_copied), android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        },
    )
}

/* ---------------- CreditsPage ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsPage(onNavigateBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val creditsList = remember {
        listOf(
            Triple("ReadYou", "GPL-3.0 License", "https://github.com/Ashinch/ReadYou"),
            Triple("Android Jetpack", "Apache License, Version 2.0", "https://github.com/androidx/androidx"),
            Triple("Kotlin", "Apache License, Version 2.0", "https://github.com/JetBrains/kotlin"),
            Triple("kotlinx.serialization", "Apache License, Version 2.0", "https://github.com/Kotlin/kotlinx.serialization"),
            Triple("OkHttp", "Apache License, Version 2.0", "https://github.com/square/okhttp"),
            Triple("Material Design 3", "Apache License, Version 2.0", "https://github.com/material-components/material-components-android"),
            Triple("Material Icons", "Apache License, Version 2.0", "https://fonts.google.com/icons"),
            Triple("Accompanist", "Apache License, Version 2.0", "https://github.com/google/accompanist"),
            Triple("ZXing", "Apache License, Version 2.0", "https://github.com/zxing/zxing"),
            Triple("App icon by Icons8", "Universal Multimedia Licensing Agreement for Icons8", "https://icons8.com/"),
        )
    }

    val uriHandler = LocalUriHandler.current

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.credits),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = { BackButton { onNavigateBack() } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding
        ) {
            items(creditsList) { credit ->
                CreditItem(
                    title = credit.first,
                    license = credit.second,
                    onClick = { uriHandler.openUri(credit.third) }
                )
            }
        }
    }
}

