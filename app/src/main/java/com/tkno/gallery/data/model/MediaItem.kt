package com.tkno.gallery.data.model

import android.net.Uri
import com.tkno.gallery.R

enum class ResolutionBadge(val iconResId: Int, val label: String) {
    RES_10K(R.drawable.ic_10k, "10K"),
    RES_8K(R.drawable.ic_8k, "8K"),
    RES_7K(R.drawable.ic_7k, "7K"),
    RES_6K(R.drawable.ic_6k, "6K"),
    RES_5K(R.drawable.ic_5k, "5K"),
    RES_4K(R.drawable.ic_4k, "4K"),
    RES_3K(R.drawable.ic_3k, "3K"),
    RES_2K(R.drawable.ic_2k, "2K"),
    RES_FHD(R.drawable.ic_fhd, "Full HD"),
    RES_HD(R.drawable.ic_hd, "HD"),
    RES_1K(R.drawable.ic_1k, "1K"),
    NONE(0, "")
}

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val path: String,
    val size: Long,
    val dateAddedSec: Long,
    val dateTakenMs: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long = 0L, // 0 for images
    val isVideo: Boolean = false,
    val mimeType: String,
    val albumName: String = "",
    val bucketId: String = "",
    val volumeName: String = "",
    val isFavorite: Boolean = false
) {
    val resolutionFormatted: String
        get() = if (width > 0 && height > 0) "${width}x${height}" else "Unknown"

    val isHighRes: Boolean
        get() = (width * height) >= 12_000_000 // 12 Megapixels or higher

    val resolutionBadge: ResolutionBadge
        get() {
            val maxDim = maxOf(width, height)
            val minDim = minOf(width, height)

            fun hasTag(title: String, tag: String): Boolean {
                val regex = Regex("(?i)(^|[^a-z0-9])${Regex.escape(tag)}($|[^a-z0-9])")
                return regex.containsMatchIn(title)
            }

            return when {
                maxDim >= 9600 || minDim >= 5400 || hasTag(name, "10K") -> ResolutionBadge.RES_10K
                maxDim >= 7680 || minDim >= 4320 || hasTag(name, "8K") -> ResolutionBadge.RES_8K
                maxDim >= 6720 || minDim >= 3780 || hasTag(name, "7K") -> ResolutionBadge.RES_7K
                maxDim >= 5760 || minDim >= 3240 || hasTag(name, "6K") -> ResolutionBadge.RES_6K
                maxDim >= 4800 || minDim >= 2700 || hasTag(name, "5K") -> ResolutionBadge.RES_5K
                minDim >= 2160 || maxDim >= 3500 || hasTag(name, "4K") || hasTag(name, "UHD") || hasTag(name, "2160p") -> ResolutionBadge.RES_4K
                maxDim >= 2880 || (maxDim >= 2560 && minDim >= 1600) || hasTag(name, "3K") -> ResolutionBadge.RES_3K
                minDim >= 1400 || (maxDim >= 1440 && minDim >= 1080 && maxDim < 1920) || hasTag(name, "2K") || hasTag(name, "QHD") || hasTag(name, "1440p") -> ResolutionBadge.RES_2K
                maxDim >= 1700 || minDim >= 950  || hasTag(name, "1080p") || hasTag(name, "FHD") || hasTag(name, "1080") -> ResolutionBadge.RES_FHD
                maxDim >= 1150 || minDim >= 650  || hasTag(name, "720p") || hasTag(name, "HD") || hasTag(name, "720")  -> ResolutionBadge.RES_HD
                maxDim >= 900  || minDim >= 500  || hasTag(name, "1K")                                               -> ResolutionBadge.RES_1K
                else -> ResolutionBadge.NONE
            }
        }
}

