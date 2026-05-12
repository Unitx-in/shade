package com.unitx.shade_core.common.config.extend

/**
 * Progress updates emitted during file operations.
 *
 * Cast to the appropriate subtype in your progress callback:
 * ```kotlin
 * onProgress = { config ->
 *     when (config) {
 *         is ProgressConfig.Copying     -> Log.d("Tag", "Copying ${config.percent}%")
 *         is ProgressConfig.Compressing -> Log.d("Tag", "Compressing ${config.percent}%")
 *     }
 * }
 * ```
 */
sealed class ProgressConfig {

    /** Emitted while copying a file to cache. */
    data class Copying(
        /** Copy progress, 0–100. */
        val percent: Int,
        /** 1-based index of the file being copied (useful in multi-select). */
        val fileNumber: Int
    ) : ProgressConfig()

    /** Emitted while compressing an image or video. */
    data class Compressing(
        /** Compression progress, 0–100. */
        val percent: Int,
        /** 1-based index of the file being compressed (useful in multi-select). */
        val fileNumber: Int
    ) : ProgressConfig()
}