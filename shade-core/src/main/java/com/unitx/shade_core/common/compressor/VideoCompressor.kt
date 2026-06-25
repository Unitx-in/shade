package com.unitx.shade_core.common.compressor

import android.content.Context
import android.media.MediaMetadataRetriever
import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.TranscoderListener
import com.otaliastudios.transcoder.resize.AtMostResizer
import com.otaliastudios.transcoder.resize.PassThroughResizer
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy
import com.unitx.shade_core.common.FileHelper
import com.unitx.shade_core.common.config.extend.ProgressConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.FileHandle
import java.io.File
import java.util.logging.FileHandler
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal object VideoCompressor {

    data class CompressionParams(
        val videoBitrate: Int = 2_000_000,
        val frameRate: Int = 30,
        val maxWidth: Int? = 720,
        val maxHeight: Int? = null,
        val keyFrameInterval: Int = 2
    )

    suspend fun compress(
        context: Context,
        inputs: List<File>,
        params: CompressionParams = CompressionParams(),
        onProgress: ((ProgressConfig.Compressing) -> Unit)? = null,
        maxFileSizeKb: Double? = null
    ): List<Result<File>> {
        return inputs.mapIndexed { index, file ->
            compress(
                context = context,
                input = file,
                params = params,
                onProgress = onProgress,
                fileNumber = index + 1
            )
        }
    }

    suspend fun compress(
        context: Context,
        input: File,
        params: CompressionParams = CompressionParams(),
        onProgress: ((ProgressConfig.Compressing) -> Unit)? = null,
        fileNumber: Int = 1,
        maxFileSizeKb: Double? = null
    ): Result<File> = suspendCancellableCoroutine { continuation ->

        val effectiveBitrate = if (maxFileSizeKb != null) {
            calculateBitrateForSize(input, maxFileSizeKb) ?: params.videoBitrate
        } else {
            params.videoBitrate
        }

        val output = File.createTempFile("VID_CMP_", ".mp4", FileHelper.shadeCacheDir(context))

        val future = Transcoder.into(output.absolutePath)
            .addDataSource(input.absolutePath)
            .setVideoTrackStrategy(
                DefaultVideoStrategy.Builder()
                    .addResizer(
                        if (params.maxWidth != null) AtMostResizer(params.maxWidth)
                        else PassThroughResizer()
                    )
                    .bitRate(effectiveBitrate.toLong())   // use calculated bitrate
                    .frameRate(params.frameRate)
                    .keyFrameInterval(params.keyFrameInterval.toFloat())
                    .build()
            )
            .setListener(object : TranscoderListener {
                override fun onTranscodeProgress(progress: Double) {
                    val percent = (progress * 100).toInt().coerceIn(0, 99)
                    onProgress?.invoke(ProgressConfig.Compressing(percent, fileNumber))
                }

                override fun onTranscodeCompleted(successCode: Int) {
                    onProgress?.invoke(ProgressConfig.Compressing(100, fileNumber))
                    continuation.resume(Result.success(output))
                }

                override fun onTranscodeCanceled() {
                    output.delete()
                    continuation.resumeWithException(
                        CancellationException("Video transcoding was cancelled")
                    )
                }

                override fun onTranscodeFailed(exception: Throwable) {
                    output.delete()
                    continuation.resume(Result.failure(exception))
                }
            })
            .transcode()

        continuation.invokeOnCancellation {
            future.cancel(true)
            output.delete()
        }
    }

    private const val AUDIO_BITRATE_BPS = 128_000  // reserve 128 kbps for audio
    private const val MIN_VIDEO_BITRATE_BPS = 100_000  // floor: 100 kbps

    private fun calculateBitrateForSize(file: File, maxFileSizeKb: Double): Int? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: return null
            retriever.release()

            if (durationMs <= 0) return null

            val durationSeconds = durationMs / 1000.0
            val totalBitrateBps = (maxFileSizeKb * 8 * 1024) / durationSeconds
            val videoBitrateBps = (totalBitrateBps - AUDIO_BITRATE_BPS)
                .toInt()
                .coerceAtLeast(MIN_VIDEO_BITRATE_BPS)

            videoBitrateBps
        } catch (e: Exception) {
            null  // fall back to params.videoBitrate
        }
    }
}