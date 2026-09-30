package com.tkno.gallery.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.OperationCanceledException
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Ultra-fast MediaStore Hardware Thumbnail Fetcher for Coil 3.
 *
 * Features:
 * 1. Bounded Parallelism (8 concurrent threads): Prevents Binder IPC & system_server saturation during rapid flings.
 * 2. Hardware CancellationSignal: Immediately aborts OS thumbnail decoding when items scroll off-screen.
 * 3. Byte-budgeted L1 High-Speed In-Memory Cache (24MB limit): Sub-millisecond synchronous bitmap retrieval on scroll revisit without memory leaks.
 * 4. Adaptive Tier Sizing: Maps Year View to fast 96x96 MICRO_KIND, Month View to 160x160, and Day Views to 256-384px.
 * 5. Full-Resolution Guard: Transparently ignores full-resolution requests (>512px or Size.ORIGINAL) allowing full-size decoders to process.
 */
class MediaStoreThumbnailFetcher(
    private val context: Context,
    private val uri: Uri,
    private val options: Options
) : Fetcher {

    companion object {
        // Dedicated thread pool with max 8 parallel queries to protect Android MediaProvider Binder IPC
        private val thumbnailDispatcher = Dispatchers.IO.limitedParallelism(8)

        // Safe 24MB Byte-measured L1 in-memory bitmap cache for instant sub-millisecond retrieval
        private const val MAX_L1_CACHE_BYTES = 24 * 1024 * 1024
        private val l1Cache = object : LruCache<String, Bitmap>(MAX_L1_CACHE_BYTES) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
        }

        fun clearL1Cache() {
            try {
                l1Cache.evictAll()
            } catch (_: Throwable) {}
        }
    }

    override suspend fun fetch(): FetchResult? = withContext(thumbnailDispatcher) {
        coroutineContext.ensureActive()

        val targetSize = resolveTargetSize()
        val cacheKey = "${uri}_${targetSize.width}x${targetSize.height}"

        // 1. Instant L1 memory cache hit
        l1Cache.get(cacheKey)?.let { cachedBitmap ->
            if (!cachedBitmap.isRecycled) {
                return@withContext ImageFetchResult(
                    image = cachedBitmap.asImage(),
                    isSampled = true,
                    dataSource = DataSource.MEMORY
                )
            }
        }

        coroutineContext.ensureActive()

        // 2. CancellationSignal linked to Coroutine lifecycle
        val signal = CancellationSignal()
        coroutineContext.job.invokeOnCompletion {
            try {
                signal.cancel()
            } catch (_: Throwable) {}
        }

        val bitmap = loadThumbnailBitmap(targetSize, signal) ?: return@withContext null

        coroutineContext.ensureActive()

        l1Cache.put(cacheKey, bitmap)

        ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = true,
            dataSource = DataSource.DISK
        )
    }

    private fun resolveTargetSize(): Size {
        val (reqW, reqH) = when {
            options.size.width is coil3.size.Dimension.Pixels && options.size.height is coil3.size.Dimension.Pixels -> {
                val w = (options.size.width as coil3.size.Dimension.Pixels).px
                val h = (options.size.height as coil3.size.Dimension.Pixels).px
                Pair(w, h)
            }
            else -> Pair(256, 256)
        }

        return when {
            reqW <= 100 -> Size(96, 96)       // Year view (10 columns) -> matches Android MICRO_KIND
            reqW <= 180 -> Size(160, 160)    // Month view (7 columns)
            reqW <= 280 -> Size(256, 256)    // Medium view (4 columns)
            else -> Size(384, 384)          // Large view (3 columns)
        }
    }

    private fun loadThumbnailBitmap(targetSize: Size, signal: CancellationSignal): Bitmap? {
        val resolver: ContentResolver = context.contentResolver

        // 1. Android 10+ (API 29+): Use ContentResolver.loadThumbnail with hardware acceleration & CancellationSignal
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                return resolver.loadThumbnail(uri, targetSize, signal)
            } catch (_: OperationCanceledException) {
                return null
            } catch (_: Throwable) {
                // Fallback
            }
        }

        // 2. Android 8 - 9 (API 26-28): Use MediaStore Thumbnails API with RGB_565 config for 50% RAM savings
        try {
            val id = ContentUris.parseId(uri)
            val uriString = uri.toString()
            val isVideo = uriString.contains("video", ignoreCase = true)
            val kind = if (targetSize.width <= 96) {
                @Suppress("DEPRECATION")
                MediaStore.Images.Thumbnails.MICRO_KIND
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Thumbnails.MINI_KIND
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val bmp = if (isVideo) {
                @Suppress("DEPRECATION")
                MediaStore.Video.Thumbnails.getThumbnail(
                    resolver,
                    id,
                    kind,
                    decodeOptions
                )
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Thumbnails.getThumbnail(
                    resolver,
                    id,
                    kind,
                    decodeOptions
                )
            }
            if (bmp != null) return bmp
        } catch (_: Throwable) {
            // Fallback
        }

        return null
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            // Intercept media content URIs (images and videos) only for thumbnail-sized requests
            val scheme = data.scheme
            val authority = data.authority
            if (scheme == ContentResolver.SCHEME_CONTENT &&
                (authority == MediaStore.AUTHORITY || authority == "media")
            ) {
                val widthDim = options.size.width
                val heightDim = options.size.height

                // Only intercept pixel-bounded thumbnail requests (<= 512px in both dimensions)
                // Unconstrained / Size.ORIGINAL / full-res requests will be handled by standard decoders
                if (widthDim is coil3.size.Dimension.Pixels && heightDim is coil3.size.Dimension.Pixels) {
                    val reqW = widthDim.px
                    val reqH = heightDim.px
                    if (reqW <= 512 && reqH <= 512) {
                        return MediaStoreThumbnailFetcher(context, data, options)
                    }
                }
            }
            return null
        }
    }
}
