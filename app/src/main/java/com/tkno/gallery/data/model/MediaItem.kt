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
        get() = (width.toLong() * height.toLong()) >= 12_000_000L // 12 Megapixels or higher

    val resolutionBadge: ResolutionBadge by lazy(LazyThreadSafetyMode.NONE) {
        computeResolutionBadge(width, height, name)
    }

    companion object {
        fun computeResolutionBadge(width: Int, height: Int, name: String): ResolutionBadge {
            val maxDim = maxOf(width, height)
            val minDim = minOf(width, height)

            if (maxDim >= 9600 || minDim >= 5400) return ResolutionBadge.RES_10K
            if (maxDim >= 7680 || minDim >= 4320) return ResolutionBadge.RES_8K
            if (maxDim >= 6720 || minDim >= 3780) return ResolutionBadge.RES_7K
            if (maxDim >= 5760 || minDim >= 3240) return ResolutionBadge.RES_6K
            if (maxDim >= 4800 || minDim >= 2700) return ResolutionBadge.RES_5K
            if (minDim >= 2160 || maxDim >= 3500) return ResolutionBadge.RES_4K
            if (maxDim >= 2880 || (maxDim >= 2560 && minDim >= 1600)) return ResolutionBadge.RES_3K
            if (minDim >= 1400 || (maxDim >= 1440 && minDim >= 1080 && maxDim < 1920)) return ResolutionBadge.RES_2K
            if (maxDim >= 1700 || minDim >= 950) return ResolutionBadge.RES_FHD
            if (maxDim >= 1150 || minDim >= 650) return ResolutionBadge.RES_HD
            if (maxDim >= 900 || minDim >= 500) return ResolutionBadge.RES_1K

            // Fast non-regex check in filename
            if (name.isNotEmpty()) {
                val upper = name.uppercase()
                if (upper.contains("10K")) return ResolutionBadge.RES_10K
                if (upper.contains("8K")) return ResolutionBadge.RES_8K
                if (upper.contains("7K")) return ResolutionBadge.RES_7K
                if (upper.contains("6K")) return ResolutionBadge.RES_6K
                if (upper.contains("5K")) return ResolutionBadge.RES_5K
                if (upper.contains("4K") || upper.contains("UHD") || upper.contains("2160P")) return ResolutionBadge.RES_4K
                if (upper.contains("3K")) return ResolutionBadge.RES_3K
                if (upper.contains("2K") || upper.contains("QHD") || upper.contains("1440P")) return ResolutionBadge.RES_2K
                if (upper.contains("1080P") || upper.contains("FHD") || upper.contains("1080")) return ResolutionBadge.RES_FHD
                if (upper.contains("720P") || upper.contains("HD") || upper.contains("720")) return ResolutionBadge.RES_HD
                if (upper.contains("1K")) return ResolutionBadge.RES_1K
            }

            return ResolutionBadge.NONE
        }
    }
}

