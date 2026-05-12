package com.unitx.shade_core.common.config.base

import com.unitx.shade_core.common.config.extend.CompressionConfig
import com.unitx.shade_core.common.config.extend.CacheConfig
import com.unitx.shade_core.common.config.extend.MultiSelectConfig
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

    internal var multiSelect: MultiSelectConfig? = null
    internal var copyToCache: CacheConfig? = null
    internal var compress: CompressionConfig? = null

    fun compress(block: CompressionConfig.() -> Unit) {
        compress = CompressionConfig().apply(block)
    }

    fun multiSelect(block: MultiSelectConfig.() -> Unit) {
        multiSelect = MultiSelectConfig().apply(block)
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