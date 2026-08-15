package com.tkno.gallery.util

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Build
import android.util.Log
import coil3.ImageLoader
import coil3.memory.MemoryCache

data class MemoryProfile(
    val totalDeviceRamBytes: Long,
    val totalDeviceRamGb: Double,
    val maxAppMemoryLimitBytes: Long,
    val maxAppMemoryLimitMb: Int,
    val imageCacheBytes: Long,
    val tierName: String
)

object MemoryManager {

    private const val TAG = "MemoryManager"

    private const val MB = 1024L * 1024L
    private const val GB = 1024L * 1024L * 1024L

    @Volatile
    private var cachedProfile: MemoryProfile? = null

    /**
     * Resolves the Device RAM Tier and configured Memory Limits:
     * - 1 GB RAM -> 200 MB App Limit
     * - 2 GB RAM -> 500 MB App Limit
     * - 3 GB RAM -> 800 MB App Limit
     * - 4 GB+ RAM -> 1024 MB (1 GB) App Limit
     */
    fun getMemoryProfile(context: Context): MemoryProfile {
        cachedProfile?.let { return it }

        val totalRam = getTotalDeviceRamBytes(context)
        val ramInGb = totalRam.toDouble() / GB.toDouble()

        val (maxAppLimitMb, tier) = when {
            ramInGb <= 1.5 -> Pair(200, "1 GB Tier (Max: 200MB)")
            ramInGb <= 2.5 -> Pair(500, "2 GB Tier (Max: 500MB)")
            ramInGb <= 3.5 -> Pair(800, "3 GB Tier (Max: 800MB)")
            else -> Pair(1024, "4+ GB Tier (Max: 1024MB / 1GB)")
        }

        val maxAppBytes = maxAppLimitMb * MB
        // Allocate 50% of the target App memory limit specifically to Coil image bitmap cache
        val imageCacheBytes = (maxAppBytes * 0.50).toLong()

        val profile = MemoryProfile(
            totalDeviceRamBytes = totalRam,
            totalDeviceRamGb = ramInGb,
            maxAppMemoryLimitBytes = maxAppBytes,
            maxAppMemoryLimitMb = maxAppLimitMb,
            imageCacheBytes = imageCacheBytes,
            tierName = tier
        )

        cachedProfile = profile
        Log.i(TAG, "Initialized Memory Profile: ${profile.tierName}, Total Device RAM: ${String.format("%.2f", ramInGb)} GB, App Memory Limit: ${maxAppLimitMb} MB, Coil Cache: ${imageCacheBytes / MB} MB")
        return profile
    }

    fun getTotalDeviceRamBytes(context: Context): Long {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            if (memInfo.totalMem > 0L) memInfo.totalMem else Runtime.getRuntime().maxMemory()
        } catch (e: Throwable) {
            Runtime.getRuntime().maxMemory()
        }
    }

    /**
     * Builds and configures the Coil ImageLoader MemoryCache according to the device RAM tier.
     */
    fun configureCoilMemoryCache(context: Context): MemoryCache {
        val profile = getMemoryProfile(context)
        return MemoryCache.Builder()
            .maxSizeBytes(profile.imageCacheBytes)
            .build()
    }

    /**
     * Handles system memory warnings to prevent OOM
     */
    @Suppress("DEPRECATION")
    fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
            level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            Log.w(TAG, "Low memory trim level received ($level). Purging non-essential caches.")
            System.gc()
        }
    }

    fun onLowMemory() {
        Log.w(TAG, "System onLowMemory event triggered. Performing emergency GC.")
        System.gc()
    }
}
