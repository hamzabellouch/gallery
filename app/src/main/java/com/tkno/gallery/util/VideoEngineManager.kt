package com.tkno.gallery.util

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory

@OptIn(UnstableApi::class)
object VideoEngineManager {

    @Volatile
    var isInitialized: Boolean = false
        private set

    lateinit var renderersFactory: DefaultRenderersFactory
        private set

    lateinit var extractorsFactory: DefaultExtractorsFactory
        private set

    fun init(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (!isInitialized) {
                val appContext = context.applicationContext
                
                // 1. Full-Spectrum Hardened Extractors Factory (MP4, MKV, WebM, TS, AVI, FLV, OGG)
                extractorsFactory = DefaultExtractorsFactory().apply {
                    setConstantBitrateSeekingEnabled(true)
                    setMp4ExtractorFlags(Mp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS)
                    setMatroskaExtractorFlags(MatroskaExtractor.FLAG_DISABLE_SEEK_FOR_CUES)
                    setTsExtractorFlags(
                        DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                                DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS
                    )
                }

                // 2. Hardware Accelerated Renderers with Intelligent Software Decoder Fallback
                renderersFactory = DefaultRenderersFactory(appContext).apply {
                    setEnableDecoderFallback(true)
                    setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                    setMediaCodecSelector(MediaCodecSelector.DEFAULT)
                }

                isInitialized = true
            }
        }
    }

    /**
     * Builds a ultra-hardened, high-performance ExoPlayer instance tailored to the device.
     */
    fun createHardenedPlayer(context: Context): ExoPlayer {
        init(context)
        val appContext = context.applicationContext

        // 3. Adaptive Dynamic LoadControl matching device RAM profile
        val memoryProfile = MemoryManager.getMemoryProfile(appContext)
        val (minBufferMs, maxBufferMs) = when (memoryProfile.maxAppMemoryLimitMb) {
            200 -> Pair(6000, 15000)
            500 -> Pair(10000, 25000)
            800 -> Pair(15000, 35000)
            else -> Pair(20000, 50000)
        }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ minBufferMs,
                /* maxBufferMs = */ maxBufferMs,
                /* bufferForPlaybackMs = */ 300, // Safe responsive start, avoids zero-buffer race conditions
                /* bufferForPlaybackAfterRebufferMs = */ 800
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // 4. MediaSourceFactory equipped with hardened extractors
        val mediaSourceFactory = DefaultMediaSourceFactory(appContext, extractorsFactory)

        // 5. Intelligent TrackSelector with capability exceed fallback (handles unusual audio/video channels)
        val trackSelector = DefaultTrackSelector(appContext).apply {
            setParameters(
                buildUponParameters()
                    .setExceedRendererCapabilitiesIfNecessary(true)
                    .setAllowMultipleAdaptiveSelections(true)
                    .setAllowAudioMixedMimeTypeAdaptiveness(true)
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
            )
        }

        // 6. Audio Attributes with automatic transient focus management
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        return ExoPlayer.Builder(appContext, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
            .build()
    }
}
