package com.unitx.shade_core.common.config

import com.unitx.shade_core.common.result.ShadeError
import com.unitx.shade_core.common.result.ShadeResult

/**
 * DSL configuration for gallery picking (images or videos).
 *
 * Single-select (default):
 * ```
 * gallery {
 *     onResult { result ->
 *         val uri = (result as ShadeResult.Single).uri
 *     }
 *     onFailure { error -> }
 * }
 * ```
 *
 * Multi-select:
 * ```
 * gallery {
 *     multiSelect(maxItems = 5)
 *     onResult { result ->
 *         val uris = (result as ShadeResult.Multiple).uris
 *     }
 *     onFailure { error -> }
 * }
 * ```
 */
class GalleryConfig {

    internal var isMultiSelect: Boolean = false
    internal var copyToCache: Boolean = false
    internal var compress: CompressionConfig? = null
    internal var maxItems: Int = Int.MAX_VALUE

    internal var onResult: ((ShadeResult) -> Unit)? = null
    internal var onFailure: ((ShadeError) -> Unit)? = null

    fun compress(block: CompressionConfig.() -> Unit) {
        compress = CompressionConfig().apply(block)
    }

    /**
     * Enable multi-select mode.
     *
     * @param maxItems Maximum number of items the user can pick.
     *   Pass [Int.MAX_VALUE] (default) to let the system decide.
     *   Note: the system picker may enforce its own lower cap on older APIs.
     */
    fun multiSelect(maxItems: Int = Int.MAX_VALUE, copyToCache: Boolean = false) {
        isMultiSelect = true
        this.maxItems = maxItems
        this.copyToCache = copyToCache
    }

    fun copyToCache(enabled: Boolean = true) {
        this.copyToCache = enabled
    }

    fun onResult(block: (ShadeResult) -> Unit) {
        onResult = block
    }

    fun onFailure(block: (ShadeError) -> Unit) {
        onFailure = block
    }
}