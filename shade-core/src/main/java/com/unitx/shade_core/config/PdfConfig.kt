package com.unitx.shade_core.config

import com.unitx.shade_core.result.ShadeError
import com.unitx.shade_core.result.ShadeResult

/**
 * DSL configuration for PDF picking.
 *
 * The result is always [ShadeResult.Single] where [ShadeResult.Single.file]
 * is non-null — Shade copies the PDF to the app cache so the caller always
 * has a stable [java.io.File] reference alongside the original [android.net.Uri].
 *
 * ```
 * pdf {
 *     onResult { result ->
 *         val uri  = result.uri
 *         val file = result.file!! // guaranteed non-null for PDF
 *     }
 *     onFailure { error -> }
 * }
 * ```
 */
class PdfConfig {
    internal var onResult: ((ShadeResult.Single) -> Unit)? = null
    internal var onFailure: ((ShadeError) -> Unit)? = null

    fun onResult(block: (ShadeResult.Single) -> Unit) {
        onResult = block
    }

    fun onFailure(block: (ShadeError) -> Unit) {
        onFailure = block
    }
}