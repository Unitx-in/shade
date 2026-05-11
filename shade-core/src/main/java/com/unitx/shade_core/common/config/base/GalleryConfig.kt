package com.unitx.shade_core.common.config.base

import com.unitx.shade_core.common.config.extend.CompressionConfig
import com.unitx.shade_core.common.config.extend.CacheConfig
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

    internal var onResult: ((ShadeResult) -> Unit)? = null
    internal var onFailure: ((ShadeError) -> Unit)? = null
    internal var isMultiSelect: Boolean = false
    internal var copyToCache: CacheConfig? = null
    internal var compress: CompressionConfig? = null
    internal var maxItems: Int = Int.MAX_VALUE

    fun compress(block: CompressionConfig.() -> Unit) {
        compress = CompressionConfig().apply(block)
    }

    fun multiSelect(maxItems: Int = Int.MAX_VALUE) {
        isMultiSelect = true
        this.maxItems = maxItems
    }

    fun copyToCache(block: CacheConfig.() -> Unit) {
        copyToCache = CacheConfig().apply(block)
    }

    fun onResult(block: (ShadeResult) -> Unit) {
        onResult = block
    }

    fun onFailure(block: (ShadeError) -> Unit) {
        onFailure = block
    }
}