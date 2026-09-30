package com.tkno.gallery.data.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class Album(
    val id: String,
    val name: String,
    val coverUri: Uri?,
    val itemCount: Int,
    val isVideoAlbum: Boolean = false,
    val isOnSdCard: Boolean = false
)
