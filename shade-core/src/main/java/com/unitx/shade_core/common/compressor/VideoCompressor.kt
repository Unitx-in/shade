package com.unitx.shade_core.common.compressor

import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.TranscoderListener
import com.otaliastudios.transcoder.resize.AtMostResizer
import com.otaliastudios.transcoder.resize.PassThroughResizer
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy
import com.unitx.shade_core.common.config.extend.ProgressConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
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
        inputs: List<File>,
        params: CompressionParams = CompressionParams(),
        onProgress: ((ProgressConfig.Compressing) -> Unit)? = null,
    ): List<File?> {
        return inputs.mapIndexed { index, file ->
            compress(
                input = file,
                params = params,
                onProgress = onProgress,
                fileNumber = index + 1
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun compress(
        input: File,
        params: CompressionParams = CompressionParams(),
        onProgress: ((ProgressConfig.Compressing) -> Unit)? = null,
        fileNumber: Int = 1,
    ): File? = suspendCancellableCoroutine { continuation ->

        val output = File.createTempFile("VID_CMP_", ".mp4", input.parentFile)

        val future = Transcoder.into(output.absolutePath)
            .addDataSource(input.absolutePath)
            .setVideoTrackStrategy(
                DefaultVideoStrategy.Builder()
                    .addResizer(
                        if (params.maxWidth != null)
                            AtMostResizer(params.maxWidth)
                        else PassThroughResizer()
                    )
                    .bitRate(params.videoBitrate.toLong())
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
                    continuation.resume(output)
                }
                override fun onTranscodeCanceled() {
                    output.delete()
                    continuation.resume(null)
                }
                override fun onTranscodeFailed(exception: Throwable) {
                    output.delete()
                    continuation.resumeWithException(exception)
                }
            })
            .transcode()

        continuation.invokeOnCancellation {
            future.cancel(true)
            output.delete()
        }
    }
}