package com.unitx.shade_core.common.config

import com.unitx.shade_core.common.result.ShadeError
import com.unitx.shade_core.common.result.ShadeResult

/**
 * DSL configuration for camera capture (image or video).
 *
 * ```
 * camera {
 *     onResult { result ->
 *         val captured = result as ShadeResult.Captured
 *         // captured.file, captured.uri
 *     }
 *     onFailure { error -> }
 * }
 * ```
 */
class CameraConfig {
    internal var onResult: ((ShadeResult.Captured) -> Unit)? = null
    internal var onFailure: ((ShadeError) -> Unit)? = null
    internal var compress: CompressionConfig? = null

    fun compress(block: CompressionConfig.() -> Unit) {
        compress = CompressionConfig().apply(block)
    }

    fun onResult(block: (ShadeResult.Captured) -> Unit) {
        onResult = block
    }

    fun onFailure(block: (ShadeError) -> Unit) {
        onFailure = block
    }


}