package com.tkno.gallery.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun SettingItem(title: String, description: String, icon: ImageVector?, onClick: () -> Unit) {
    PreferenceItem(
        title = title,
        description = description,
        icon = icon,
        onClick = onClick
    )
}
