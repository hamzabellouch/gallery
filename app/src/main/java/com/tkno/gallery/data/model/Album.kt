package com.tkno.gallery.data.model

import android.net.Uri

data class Album(
    val id: String,
    val name: String,
    val coverUri: Uri?,
    val itemCount: Int,
    val isVideoAlbum: Boolean = false,
    val isOnSdCard: Boolean = false
)
