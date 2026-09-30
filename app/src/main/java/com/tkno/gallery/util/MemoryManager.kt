package com.tkno.gallery.util

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.util.Log
import coil3.SingletonImageLoader
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
     * Resolves the Device RAM Tier and JVM heap-safe configured Memory Limits:
     * Calculates Coil memory cache size based on JVM Heap max ceiling Runtime.getRuntime().maxMemory()
     * (25% of available JVM heap) rather than device total RAM, to completely prevent OutOfMemoryError.
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

        // Calculate Coil memory cache based on JVM Heap max ceiling (25% of available JVM heap ceiling)
        val jvmMaxHeap = Runtime.getRuntime().maxMemory().coerceAtLeast(32L * MB)
        val imageCacheBytes = (jvmMaxHeap * 0.25).toLong().coerceIn(16L * MB, 128L * MB)

        val profile = MemoryProfile(
            totalDeviceRamBytes = totalRam,
            totalDeviceRamGb = ramInGb,
            maxAppMemoryLimitBytes = maxAppBytes,
            maxAppMemoryLimitMb = maxAppLimitMb,
            imageCacheBytes = imageCacheBytes,
            tierName = tier
        )

        cachedProfile = profile
        Log.i(TAG, "Initialized Memory Profile: ${profile.tierName}, Total Device RAM: ${String.format("%.2f", ramInGb)} GB, Max JVM Heap: ${jvmMaxHeap / MB} MB, Coil Cache: ${imageCacheBytes / MB} MB")
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
     * Builds and configures the Coil ImageLoader MemoryCache according to safe JVM Heap limits.
     */
    fun configureCoilMemoryCache(context: Context): MemoryCache {
        val profile = getMemoryProfile(context)
        return MemoryCache.Builder()
            .maxSizeBytes(profile.imageCacheBytes)
            .build()
    }

    /**
     * Handles system memory warnings to purge caches and prevent OOM
     */
    @Suppress("DEPRECATION")
    fun onTrimMemory(context: Context?, level: Int) {
        try {
            // 1. Clear high-speed L1 bitmap cache in MediaStoreThumbnailFetcher
            MediaStoreThumbnailFetcher.clearL1Cache()

            // 2. Trim or Clear Coil Memory Cache based on trim severity
            if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
                level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE ||
                level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
            ) {
                Log.w(TAG, "Critical memory trim ($level). Purging all Coil memory caches.")
                if (context != null) {
                    SingletonImageLoader.get(context).memoryCache?.clear()
                }
                System.gc()
            } else if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
                level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE ||
                level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
            ) {
                Log.d(TAG, "Moderate memory trim ($level). Trimming Coil memory cache by 50%.")
                if (context != null) {
                    val memoryCache = SingletonImageLoader.get(context).memoryCache
                    if (memoryCache != null) {
                        memoryCache.trimToSize(memoryCache.size / 2)
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error handling onTrimMemory", e)
        }
    }

    fun onTrimMemory(level: Int) {
        onTrimMemory(null, level)
    }

    fun onLowMemory(context: Context? = null) {
        Log.w(TAG, "System onLowMemory event triggered. Performing emergency cache purge.")
        try {
            MediaStoreThumbnailFetcher.clearL1Cache()
            if (context != null) {
                SingletonImageLoader.get(context).memoryCache?.clear()
            }
            System.gc()
        } catch (e: Throwable) {
            Log.e(TAG, "Error handling onLowMemory", e)
        }
    }
}
