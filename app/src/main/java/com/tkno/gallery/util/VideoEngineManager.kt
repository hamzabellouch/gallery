package com.tkno.gallery.util

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector

@OptIn(UnstableApi::class)
object VideoEngineManager {

    @Volatile
    var isInitialized: Boolean = false
        private set

    lateinit var renderersFactory: DefaultRenderersFactory
        private set

    fun init(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (!isInitialized) {
                val appContext = context.applicationContext
                renderersFactory = DefaultRenderersFactory(appContext).apply {
                    setEnableDecoderFallback(true)
                    setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                    setMediaCodecSelector(MediaCodecSelector.DEFAULT)
                }
                isInitialized = true
            }
        }
    }
}
