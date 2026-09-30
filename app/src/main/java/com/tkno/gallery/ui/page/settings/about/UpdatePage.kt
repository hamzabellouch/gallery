package com.tkno.gallery.ui.page.settings.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tkno.gallery.R
import com.tkno.gallery.ui.component.BackButton
import com.tkno.gallery.ui.component.PreferenceInfo
import com.tkno.gallery.ui.component.PreferenceSubtitle
import com.tkno.gallery.ui.component.PreferenceSwitchWithContainer
import com.tkno.gallery.ui.page.UpdateDialog
import com.tkno.gallery.util.UpdateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatePage(
    onNavigateBack: () -> Unit,
    triggerUpdate: Boolean = false
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("gallery_prefs", android.content.Context.MODE_PRIVATE) }

    var autoUpdate by remember { mutableStateOf(try { prefs.getBoolean("auto_update_enabled", false) } catch (e: Throwable) { false }) }
    var updateChannel by remember { mutableStateOf(try { prefs.getInt("update_channel", 1) } catch (e: Throwable) { 1 }) }
    var bellEnabled by remember { mutableStateOf(try { prefs.getBoolean("update_bell_enabled", true) } catch (e: Throwable) { true }) }

    var release by remember { mutableStateOf(UpdateUtil.Release()) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(triggerUpdate) {
        if (triggerUpdate && !isLoading) {
            scope.launch {
                isLoading = true
                runCatching {
                    val includePrerelease = updateChannel == 1
                    val foundRelease = withContext(Dispatchers.IO) {
                        UpdateUtil.checkForUpdate(context, includePrerelease = includePrerelease)
                    }
                    if (foundRelease != null) {
                        release = foundRelease
                        showUpdateDialog = true
                    } else {
                        android.widget.Toast.makeText(context, context.getString(R.string.app_up_to_date), android.widget.Toast.LENGTH_SHORT).show()
                    }
                }.onFailure {
                    it.printStackTrace()
                    android.widget.Toast.makeText(context, context.getString(R.string.app_update_failed), android.widget.Toast.LENGTH_SHORT).show()
                }
                isLoading = false
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            var menuExpanded by remember { mutableStateOf(false) }
            LargeTopAppBar(
                title = {
                    Text(modifier = Modifier, text = stringResource(id = R.string.auto_update), color = MaterialTheme.colorScheme.onBackground)
                },
                navigationIcon = { BackButton { onNavigateBack() } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = {
                        bellEnabled = !bellEnabled
                        prefs.edit().putBoolean("update_bell_enabled", bellEnabled).apply()
                    }) {
                        Icon(
                            imageVector = if (bellEnabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                            contentDescription = "Toggle Update Notifications",
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "More actions",
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Folder,
                                        contentDescription = null,
                                    )
                                },
                                text = { Text(stringResource(id = R.string.update_directory)) },
                                onClick = {
                                    menuExpanded = false
                                    runCatching {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    }.onFailure {
                                        android.widget.Toast.makeText(context, "Folder picker not available", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        content = { paddings ->
            LazyColumn(modifier = Modifier.padding(paddings)) {
                item {
                    PreferenceSwitchWithContainer(
                        title = stringResource(id = R.string.enable_auto_update),
                        icon = null,
                        isChecked = autoUpdate,
                    ) {
                        autoUpdate = !autoUpdate
                        prefs.edit().putBoolean("auto_update_enabled", autoUpdate).apply()
                    }
                }
                item {
                    PreferenceSubtitle(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        text = stringResource(id = R.string.update_channel),
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Stable (Currently unavailable)
                        Row(
                            modifier = Modifier.weight(1f).alpha(0.38f).padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = false, onClick = null, enabled = false)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(id = R.string.stable),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(id = R.string.currently_unavailable),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        // Preview (early access)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    updateChannel = 1
                                    prefs.edit().putInt("update_channel", 1).apply()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = updateChannel == 1,
                                onClick = {
                                    updateChannel = 1
                                    prefs.edit().putInt("update_channel", 1).apply()
                                },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(id = R.string.preview),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(id = R.string.early_access),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                item {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ProgressIndicatorButton(
                            modifier = Modifier.padding(horizontal = 24.dp).padding(top = 6.dp).padding(bottom = 12.dp),
                            text = stringResource(id = R.string.check_for_updates),
                            icon = Icons.Outlined.Update,
                            isLoading = isLoading,
                        ) {
                            if (!isLoading) {
                                scope.launch {
                                    isLoading = true
                                    runCatching {
                                        val includePrerelease = updateChannel == 1
                                        val foundRelease = withContext(Dispatchers.IO) {
                                            UpdateUtil.checkForUpdate(context, includePrerelease = includePrerelease)
                                        }
                                        if (foundRelease != null) {
                                            release = foundRelease
                                            showUpdateDialog = true
                                        } else {
                                            android.widget.Toast.makeText(context, context.getString(R.string.app_up_to_date), android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }.onFailure {
                                        it.printStackTrace()
                                        android.widget.Toast.makeText(context, context.getString(R.string.app_update_failed), android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    isLoading = false
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                }
                item {
                    PreferenceInfo(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        text = stringResource(id = R.string.pre_release_desc),
                    )
                }
            }
        },
    )

    if (showUpdateDialog) {
        UpdateDialog(onDismissRequest = { showUpdateDialog = false }, release = release)
    }
}

@Composable
fun ProgressIndicatorButton(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        modifier = modifier,
        onClick = onClick,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        if (isLoading) {
            Box(modifier = Modifier.size(18.dp)) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp).align(Alignment.Center),
                    strokeWidth = 2.dp,
                    strokeCap = StrokeCap.Round,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                )
            }
        } else {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Text(text = text, modifier = Modifier.padding(start = 8.dp))
    }
}
