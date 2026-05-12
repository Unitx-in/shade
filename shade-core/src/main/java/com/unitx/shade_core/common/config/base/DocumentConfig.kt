package com.unitx.shade_core.common.config.base

import com.unitx.shade_core.common.config.extend.CacheConfig
import com.unitx.shade_core.common.config.extend.MultiSelectConfig
import com.unitx.shade_core.common.result.ShadeError
import com.unitx.shade_core.common.result.ShadeResult

/**
 * DSL configuration for document picking.
 *
 * MIME types are supplied at the call-site via [ShadeAction.Document], not here.
 * This config holds result/failure callbacks, optional cache copying, and
 * optional multi-select behaviour.
 *
 * By default Shade does **not** copy documents to cache — [ShadeResult.Single.file]
 * will be `null` and only the URI is available. Enable [copyToCache] if you need
 * a stable [java.io.File] reference (e.g. for upload or processing).
 *
 * ## Single select (default)
 * ```kotlin
 * document {
 *     copyToCache {
 *         enabled = true
 *         onProgress = { config ->
 *             config as ProgressConfig.Copying
 *             Log.d("Tag", "Copying ${config.percent}%")
 *         }
 *     }
 *     onResult { result ->
 *         val single = result as ShadeResult.Single
 *         // single.uri  — always available
 *         // single.file — non-null only when copyToCache is enabled
 *     }
 *     onFailure { error -> }
 * }
 * ```
 *
 * ## Multi select
 * ```kotlin
 * document {
 *     multiSelect {
 *         enabled = true
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
 * @see ShadeAction.Document
 * @see CacheConfig
 * @see MultiSelectConfig
 * @see ShadeResult
 * @see ShadeError
 */
class DocumentConfig {

    internal var onResult: ((ShadeResult) -> Unit)? = null
    internal var onFailure: ((ShadeError) -> Unit)? = null
    internal var copyToCache: CacheConfig? = null
    internal var multiSelect: MultiSelectConfig? = null

    /**
     * Called when one or more documents are picked successfully.
     *
     * Cast the result based on your configuration:
     * - [ShadeResult.Single] — when multi-select is not enabled
     * - [ShadeResult.Multiple] — when multi-select is enabled
     */
    fun onResult(block: (ShadeResult) -> Unit) {
        onResult = block
    }

    /**
     * Enables multi-document selection.
     *
     * When enabled, the system picker allows selecting multiple documents
     * and the result is delivered as [ShadeResult.Multiple].
     *
     * Note: unlike image/video multi-select, `maxItems` cannot be enforced
     * by the system document picker — the user may select any number of files.
     */
    fun multiSelect(block: MultiSelectConfig.() -> Unit) {
        multiSelect = MultiSelectConfig().apply(block)
    }

    /**
     * Called when picking fails or is cancelled.
     *
     * Possible errors:
     * - [ShadeError.PickCancelled] — user dismissed the picker
     * - [ShadeError.FileSaveFailed] — copyToCache was enabled but the file could not be saved
     */
    fun onFailure(block: (ShadeError) -> Unit) {
        onFailure = block
    }

    /**
     * Copies the picked document(s) to the app's cache directory,
     * providing a stable [java.io.File] reference in the result.
     *
     * Without this, only the URI is available and it may not be
     * accessible outside the picker's granted permission window.
     */
    fun copyToCache(block: CacheConfig.() -> Unit) {
        copyToCache = CacheConfig().apply(block)
    }
}