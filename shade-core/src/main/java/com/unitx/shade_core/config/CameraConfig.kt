package com.unitx.shade_core.config

import com.unitx.shade_core.result.ShadeError
import com.unitx.shade_core.result.ShadeResult

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

    fun onResult(block: (ShadeResult.Captured) -> Unit) {
        onResult = block
    }

    fun onFailure(block: (ShadeError) -> Unit) {
        onFailure = block
    }
}