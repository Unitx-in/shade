package com.unitx.shade_core.common.config.base

import com.unitx.shade_core.common.config.extend.CompressionConfig
import com.unitx.shade_core.common.config.extend.CacheConfig
import com.unitx.shade_core.common.config.extend.MultiSelectConfig
import com.unitx.shade_core.common.result.ShadeError
import com.unitx.shade_core.common.result.ShadeResult

/**
 * DSL configuration for gallery picking (images or videos).
 *
 * Attach this block inside `image { gallery { } }` or `video { gallery { } }`.
 * Defaults to single-select. Enable [multiSelect] for picking multiple items.
 *
 * ## Single select
 * ```kotlin
 * gallery {
 *     onResult { result ->
 *         val single = result as ShadeResult.Single
 *         // single.uri, single.file
 *     }
 *     onFailure { error -> }
 * }
 * ```
 *
 * ## Multi select
 * ```kotlin
 * gallery {
 *     multiSelect {
 *         enabled = true
 *         maxItems = 5
 *     }
 *     onResult { result ->
 *         val multiple = result as ShadeResult.Multiple
 *         multiple.items.forEach { item ->
 *             // item.uri, item.file
 *         }
 *     }
 *     onFailure { error -> }
 * }
 * ```
 *
 * ## With compression and cache copy
 * ```kotlin
 * gallery {
 *     compress {
 *         enabled = true
 *         quality = 80
 *         maxWidth = 1024
 *     }
 *     copyToCache {
 *         enabled = true
 *         onProgress = { config ->
 *             config as ProgressConfig.Copying
 *             Log.d("Tag", "File ${config.fileNumber}: ${config.percent}%")
 *         }
 *     }
 *     onResult { result -> }
 *     onFailure { error -> }
 * }
 * ```
 *
 * Note: `compress` and `copyToCache` are mutually exclusive — when compression
 * is enabled it takes precedence and `copyToCache` is ignored.
 *
 * @see CompressionConfig
 * @see CacheConfig
 * @see MultiSelectConfig
 * @see ShadeResult
 * @see ShadeError
 */
class GalleryConfig {

    internal var onResult: ((ShadeResult) -> Unit)? = null
    internal var onFailure: ((ShadeError) -> Unit)? = null
    internal var multiSelect: MultiSelectConfig? = null
    internal var copyToCache: CacheConfig? = null
    internal var compress: CompressionConfig? = null

    /**
     * Configures image/video compression applied after picking.
     *
     * When enabled, each picked file is compressed before being delivered
     * to [onResult]. If compression fails, [onFailure] is invoked with
     * [ShadeError.CompressionFailed]. Takes precedence over [copyToCache].
     */
    fun compress(block: CompressionConfig.() -> Unit) {
        compress = CompressionConfig().apply(block)
    }

    /**
     * Enables multi-item selection from the gallery.
     *
     * Result is delivered as [ShadeResult.Multiple].
     * If not called, the gallery defaults to single-select
     * and result is delivered as [ShadeResult.Single].
     */
    fun multiSelect(block: MultiSelectConfig.() -> Unit) {
        multiSelect = MultiSelectConfig().apply(block)
    }

    /**
     * Copies the picked file(s) to the app's cache directory,
     * providing stable [java.io.File] references in the result.
     *
     * Ignored when [compress] is enabled, since compression already
     * produces a cached file as its output.
     */
    fun copyToCache(block: CacheConfig.() -> Unit) {
        copyToCache = CacheConfig().apply(block)
    }

    /**
     * Called when one or more items are picked successfully.
     *
     * Cast the result based on your configuration:
     * - [ShadeResult.Single] — when multi-select is not enabled
     * - [ShadeResult.Multiple] — when multi-select is enabled
     */
    fun onResult(block: (ShadeResult) -> Unit) {
        onResult = block
    }

    /**
     * Called when picking fails or is cancelled.
     *
     * Possible errors:
     * - [ShadeError.PickCancelled] — user dismissed the picker
     * - [ShadeError.CompressionFailed] — compression was enabled but failed
     * - [ShadeError.FileSaveFailed] — copyToCache was enabled but the file could not be saved
     */
    fun onFailure(block: (ShadeError) -> Unit) {
        onFailure = block
    }
}