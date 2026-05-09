package com.unitx.shade_core.common.config

import com.unitx.shade_core.common.result.ShadeError
import com.unitx.shade_core.common.result.ShadeResult

/**
 * DSL configuration for generic document picking (Word, Excel, text, etc.).
 *
 * MIME types are supplied at the [ShadeAction.Document] call-site, not here —
 * this config only holds callbacks and optional copy-to-cache behaviour.
 *
 * By default Shade does **not** copy documents to cache (unlike PDF) because
 * documents can be large. Set [copyToCache] = true if you need a stable [File].
 *
 * ```
 * document {
 *     copyToCache = true          // optional — gives you ShadeResult.Single.file
 *     onResult { result ->
 *         val uri  = result.uri
 *         val file = result.file  // non-null only when copyToCache = true
 *     }
 *     onFailure { error -> }
 * }
 * ```
 */
class DocumentConfig {

    /**
     * When true, Shade copies the picked document into [Context.cacheDir]
     * and populates [ShadeResult.Single.file]. Defaults to false.
     */
    internal var copyToCache: Boolean = false

    internal var onResult: ((ShadeResult.Single) -> Unit)? = null
    internal var onFailure: ((ShadeError) -> Unit)? = null

    fun onResult(block: (ShadeResult.Single) -> Unit) {
        onResult = block
    }

    fun onFailure(block: (ShadeError) -> Unit) {
        onFailure = block
    }

    fun copyToCache(enabled: Boolean) {
        copyToCache = enabled
    }

}