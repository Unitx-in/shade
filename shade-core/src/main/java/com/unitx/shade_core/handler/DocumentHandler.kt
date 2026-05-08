package com.unitx.shade_core.handler

import android.content.Context
import com.unitx.shade_core.common.DocumentMimeType
import com.unitx.shade_core.common.FileHelper
import com.unitx.shade_core.action.ShadeAction
import com.unitx.shade_core.config.ShadeConfig
import com.unitx.shade_core.core.LauncherRegistry
import com.unitx.shade_core.result.ShadeError
import com.unitx.shade_core.result.ShadeResult

/**
 * Handles PDF and generic document picking flows.
 *
 * Responsibilities:
 * - Launching the system file picker with appropriate MIME types
 * - Copying content to cache when [DocumentConfig.copyToCache] is true
 * - Dispatching [ShadeResult.Single] or [ShadeError] to the DSL config callbacks
 */
internal class DocumentHandler(
    private val context: Context,
    private val config: ShadeConfig,
    private val registry: LauncherRegistry
) {

    init {
        registry.onPdfResult = { uri ->
            if (uri == null) {
                config.pdf?.onFailure?.invoke(ShadeError.PickCancelled)
            } else {
                val file = FileHelper.copyUriToCache(context, uri, "PDF_", ".pdf")
                if (file != null)
                    config.pdf?.onResult?.invoke(ShadeResult.Single(uri, file))
                else
                    config.pdf?.onFailure?.invoke(ShadeError.FileSaveFailed)
            }
        }

        registry.onDocumentResult = { uri ->
            val docConfig = config.document
            if (uri == null) {
                docConfig?.onFailure?.invoke(ShadeError.PickCancelled)
            } else {
                val file = if (docConfig?.copyToCache == true)
                    FileHelper.copyUriToCache(context, uri, "DOC_", FileHelper.extensionFromUri(context, uri))
                else null

                if (docConfig?.copyToCache == true && file == null)
                    docConfig.onFailure?.invoke(ShadeError.FileSaveFailed)
                else
                    docConfig?.onResult?.invoke(ShadeResult.Single(uri, file))
            }
        }
    }

    fun handlePdf() {
        config.pdf ?: return
        registry.pdfPickerLauncher.launch(arrayOf("application/pdf"))
    }

    fun handleDocument(action: ShadeAction.Document) {
        config.document ?: return
        val mimeTypes = action.mimeTypes
            .takeIf { it.isNotEmpty() }
            ?.toTypedArray()
            ?: DocumentMimeType.ALL_VALUES
        registry.documentPickerLauncher.launch(mimeTypes)
    }
}