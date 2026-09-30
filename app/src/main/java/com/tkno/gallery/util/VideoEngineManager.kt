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
                    setMp4ExtractorFlags(
                        Mp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS or
                                Mp4Extractor.FLAG_READ_SEF_DATA
                    )
                    setMatroskaExtractorFlags(MatroskaExtractor.FLAG_DISABLE_SEEK_FOR_CUES)
                    setTsExtractorFlags(
                        DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                                DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS
                    )
                }

                // 2. Hardware Accelerated Renderers with Asynchronous MediaCodec Queueing & Decoder Fallback
                renderersFactory = DefaultRenderersFactory(appContext).apply {
                    setEnableDecoderFallback(true)
                    setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                    setMediaCodecSelector(MediaCodecSelector.DEFAULT)
                    forceEnableMediaCodecAsynchronousQueueing()
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

        // 3. Adaptive Dynamic LoadControl with uncapped target buffer bytes for high-bitrate 4K / 8K video
        val memoryProfile = MemoryManager.getMemoryProfile(appContext)
        val (minBufferMs, maxBufferMs) = when (memoryProfile.maxAppMemoryLimitMb) {
            200 -> Pair(10000, 25000)
            500 -> Pair(15000, 35000)
            800 -> Pair(20000, 50000)
            else -> Pair(25000, 60000)
        }

        val loadControl = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(
                /* minBufferMs = */ minBufferMs,
                /* maxBufferMs = */ maxBufferMs,
                /* bufferForPlaybackMs = */ 1500, // Rock-solid start buffer so high bitrate 4K/8K has ample frames ready
                /* bufferForPlaybackAfterRebufferMs = */ 2500
            )
            .setTargetBufferBytes(C.LENGTH_UNSET) // Dynamic capacity based on actual bitrate, avoiding 13MB ceiling for high-bitrate video
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(10000, true)
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
            .build()
    }
}
