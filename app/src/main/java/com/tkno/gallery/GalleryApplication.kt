package com.tkno.gallery

import android.app.Application
import android.os.Build
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import coil3.video.VideoFrameDecoder
import com.tkno.gallery.util.MediaStoreThumbnailFetcher
import com.tkno.gallery.util.MemoryManager

class GalleryApplication : App(), SingletonImageLoader.Factory {

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryManager.configureCoilMemoryCache(context)
            }
            .components {
                // 1. Ultra-fast hardware thumbnail fetcher for MediaStore images & videos (1-2ms)
                add(MediaStoreThumbnailFetcher.Factory(context))

                // 2. Standalone decoders for Video Thumbnails fallback, Animated GIFs, SVGs
                add(VideoFrameDecoder.Factory())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
            }
            .crossfade(false)
            .build()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        MemoryManager.onTrimMemory(level)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        MemoryManager.onLowMemory()
    }
}

