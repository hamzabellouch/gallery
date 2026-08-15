package com.tkno.gallery.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FavoritesManager(context: Context) {
    private val prefs = context.getSharedPreferences("gallery_favorites", Context.MODE_PRIVATE)

    private val _favoriteUris = MutableStateFlow<Set<String>>(loadFavorites())
    val favoriteUris: StateFlow<Set<String>> = _favoriteUris.asStateFlow()

    private fun loadFavorites(): Set<String> {
        return prefs.getStringSet("favorite_uris", emptySet()) ?: emptySet()
    }

    fun toggleFavorite(uriString: String) {
        val current = _favoriteUris.value.toMutableSet()
        if (current.contains(uriString)) {
            current.remove(uriString)
        } else {
            current.add(uriString)
        }
        prefs.edit().putStringSet("favorite_uris", current).apply()
        _favoriteUris.value = current
    }

    fun isFavorite(uriString: String): Boolean {
        return _favoriteUris.value.contains(uriString)
    }
}
