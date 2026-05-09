package com.unitx.shade_core.common.compressor

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Lightweight video compressor abstraction.
 *
 * Currently delegates to FFmpeg-based compression.
 */
internal object VideoCompressor {

    data class CompressionParams(
        val videoBitrate: Int = 2_000_000,
        val frameRate: Int = 30,
        val maxWidth: Int? = 720,
        val maxHeight: Int? = null,
        val keyFrameInterval: Int = 2
    )

    /**
     * Compresses a video file.
     *
     * Returns compressed file or null on failure.
     */
    suspend fun compress(
        input: File,
        params: CompressionParams = CompressionParams()
    ): File? = withContext(Dispatchers.IO) {

        try {

            val output = File.createTempFile(
                "VID_CMP_",
                ".mp4",
                input.parentFile
            )

            val success = compressWithFfmpeg(
                input = input,
                output = output,
                params = params
            )

            if (success) output
            else {
                output.delete()
                null
            }

        } catch (e: Exception) {

            if (e is CancellationException) {
                throw e
            }

            e.printStackTrace()
            null
        }
    }

    /**
     * FFmpeg compression implementation.
     *
     * NOTE:
     * This assumes FFmpeg is available in your project.
     * Replace implementation based on your FFmpeg library.
     */
    private fun compressWithFfmpeg(
        input: File,
        output: File,
        params: CompressionParams
    ): Boolean {

        return try {

            val scaleFilter = buildString {

                params.maxWidth?.let { maxWidth ->

                    append(
                        "scale='min($maxWidth,iw)':-2"
                    )
                }
            }

            val command = mutableListOf(
                "ffmpeg",
                "-i", input.absolutePath,
                "-c:v", "libx264",
                "-b:v", params.videoBitrate.toString(),
                "-r", params.frameRate.toString(),
                "-g", (params.keyFrameInterval * params.frameRate).toString(),
            )

            if (scaleFilter.isNotBlank()) {
                command += listOf("-vf", scaleFilter)
            }

            command += listOf(
                "-c:a", "copy",
                "-y",
                output.absolutePath
            )

            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()

            exitCode == 0

        } catch (e: Exception) {

            e.printStackTrace()
            false
        }
    }
}