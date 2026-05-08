package com.unitx.shade_core.compose.handler

import androidx.activity.result.ActivityResultLauncher
import com.unitx.shade_core.action.ShadeAction
import com.unitx.shade_core.common.DocumentMimeType
import com.unitx.shade_core.compose.state.ShadeResultHolder
import com.unitx.shade_core.config.ShadeConfig
import com.unitx.shade_core.result.ShadeError
import com.unitx.shade_core.result.ShadeResult

/**
 * Compose-side document handler.
 *
 * Wires PDF and document result callbacks and exposes
 * [handlePdf] / [handleDocument] to [ComposeShadeCore].
 */
internal class ComposeDocumentHandler(
    private val config: ShadeConfig,
    private val pdfLauncher: ActivityResultLauncher<Array<String>>?,
    private val documentLauncher: ActivityResultLauncher<Array<String>>?,
    pdfCallback: ShadeResultHolder,
    documentCallback: ShadeResultHolder,
) {

    init {
        // ── PDF result ────────────────────────────────────────────────────────
        pdfCallback.onResult  = { config.pdf?.onResult?.invoke(it as ShadeResult.Single) }
        pdfCallback.onFailure = { config.pdf?.onFailure?.invoke(it) }

        // ── Document result ───────────────────────────────────────────────────
        documentCallback.onResult  = { config.document?.onResult?.invoke(it as ShadeResult.Single) }
        documentCallback.onFailure = { config.document?.onFailure?.invoke(it) }
    }

    fun handlePdf() {
        config.pdf ?: return
        pdfLauncher?.launch(arrayOf("application/pdf"))
    }

    fun handleDocument(action: ShadeAction.Document) {
        config.document ?: return
        val mimes = action.mimeTypes
            .takeIf { it.isNotEmpty() }
            ?.toTypedArray()
            ?: DocumentMimeType.ALL_VALUES
        documentLauncher?.launch(mimes)
    }
}