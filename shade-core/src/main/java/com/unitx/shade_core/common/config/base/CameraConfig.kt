package com.unitx.shade_core.common.config.base

import com.unitx.shade_core.common.config.extend.CompressionConfig
import com.unitx.shade_core.common.config.extend.ProgressConfig
import com.unitx.shade_core.common.result.ShadeError
import com.unitx.shade_core.common.result.ShadeResult

/**
 * DSL configuration for camera capture (image or video).
 *
 * Attach this block inside `image { camera { } }` or `video { camera { } }`.
 * Shade requests camera permission automatically if not already granted.
 *
 * ## Usage
 * ```kotlin
 * camera {
 *     compress {
 *         enabled = true
 *         quality = 80
 *         maxWidth = 1024
 *         maxHeight = 1024
 *     }
 *     onResult { result ->
 *         // result.file — always non-null for camera captures
 *         // result.uri  — content URI pointing to the captured file
 *     }
 *     onFailure { error -> }
 * }
 * ```
 *
 * @see CompressionConfig
 * @see ShadeResult.Captured
 * @see ShadeError
 */
class CameraConfig {
    internal var onResult: ((ShadeResult.Captured) -> Unit)? = null
    internal var onFailure: ((ShadeError) -> Unit)? = null
    internal var compress: CompressionConfig? = null

    /**
     * Configures image/video compression for camera captures.
     *
     * When enabled, the captured file is compressed before being
     * delivered to [onResult]. If compression fails, [onFailure]
     * is invoked with [ShadeError.CompressionFailed].
     */
    fun compress(block: CompressionConfig.() -> Unit) {
        compress = CompressionConfig().apply(block)
    }

    /**
     * Called when capture completes successfully.
     *
     * [ShadeResult.Captured.file] is always non-null for camera results.
     * [ShadeResult.Captured.uri] is a content URI valid for the lifetime of the app.
     */
    fun onResult(block: (ShadeResult.Captured) -> Unit) {
        onResult = block
    }

    /**
     * Called when capture fails or is cancelled.
     *
     * Possible errors:
     * - [ShadeError.PermissionDenied] — camera permission denied but can be re-requested
     * - [ShadeError.PermissionPermanentlyDenied] — user selected "Don't ask again"
     * - [ShadeError.CompressionFailed] — compression was enabled but failed
     * - [ShadeError.FileCreationFailed] — temp file could not be created
     */
    fun onFailure(block: (ShadeError) -> Unit) {
        onFailure = block
    }
}