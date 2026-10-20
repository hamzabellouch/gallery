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
import androidx.media3.exoplayer.upstream.DefaultAllocator
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
                    setConstantBitrateSeekingEnabled(false) // Do not assume CBR for high-bitrate VBR videos
                    // Preserving Edit Lists is crucial for audio/video synchronization (e.g. LATM/AAC audio with H.264 60fps)
                    setMp4ExtractorFlags(Mp4Extractor.FLAG_READ_SEF_DATA)
                    setMatroskaExtractorFlags(MatroskaExtractor.FLAG_DISABLE_SEEK_FOR_CUES)
                    setTsExtractorFlags(
                        DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                                DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS
                    )
                }

                // 2. Hardware Accelerated Renderers with Safe Queueing & Decoder Fallback
                renderersFactory = DefaultRenderersFactory(appContext).apply {
                    setEnableDecoderFallback(true)
                    setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                    setMediaCodecSelector(MediaCodecSelector.DEFAULT)
                    // Let Media3 manage async queueing dynamically per device to avoid frame drops on high-FPS AVC
                    setAllowedVideoJoiningTimeMs(5000)
                }

                isInitialized = true
            }
        }
    }

    /**
     * Builds an ultra-hardened, high-performance ExoPlayer instance tailored to 4K / 8K 60fps/120fps video.
     */
    fun createHardenedPlayer(context: Context): ExoPlayer {
        init(context)
        val appContext = context.applicationContext

        // 3. Adaptive Dynamic LoadControl tailored for low-latency, zero-stutter local storage playback
        val memoryProfile = MemoryManager.getMemoryProfile(appContext)
        val (minBufferMs, maxBufferMs, bufferForPlaybackMs) = when (memoryProfile.maxAppMemoryLimitMb) {
            200 -> Triple(2000, 4000, 400)
            500 -> Triple(2500, 5000, 300)
            else -> Triple(3000, 6000, 250)
        }

        val loadControl = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(
                /* minBufferMs = */ minBufferMs,
                /* maxBufferMs = */ maxBufferMs,
                /* bufferForPlaybackMs = */ bufferForPlaybackMs, // Near-instantaneous startup from flash storage
                /* bufferForPlaybackAfterRebufferMs = */ 500
            )
            .setTargetBufferBytes(C.LENGTH_UNSET) // Dynamic capacity avoiding artificial ceilings on 4K/8K 120fps
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(2000, false) // 2s back buffer without retaining full GOP keyframe clusters to prevent GC spikes
            .build()

        // 4. MediaSourceFactory equipped with hardened extractors
        val mediaSourceFactory = DefaultMediaSourceFactory(appContext, extractorsFactory)

        // 5. Intelligent TrackSelector with unconstrained resolution/framerate and capability exceed fallback
        val trackSelector = DefaultTrackSelector(appContext).apply {
            setParameters(
                buildUponParameters()
                    .setExceedRendererCapabilitiesIfNecessary(true)
                    .setAllowMultipleAdaptiveSelections(true)
                    .setAllowAudioMixedMimeTypeAdaptiveness(true)
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowVideoNonSeamlessAdaptiveness(true)
                    .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                    .setMaxVideoFrameRate(Int.MAX_VALUE)
                    .setMaxVideoBitrate(Int.MAX_VALUE)
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
            .setVideoChangeFrameRateStrategy(C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
}
