package com.unitx.shade_core.handler

import android.content.Context
import android.net.Uri
import com.unitx.shade_core.common.DocumentMimeType
import com.unitx.shade_core.common.FileHelper
import com.unitx.shade_core.common.action.ShadeAction
import com.unitx.shade_core.common.config.ShadeConfig
import com.unitx.shade_core.core.LauncherRegistry
import com.unitx.shade_core.common.result.ShadeError
import com.unitx.shade_core.common.result.ShadeResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
    private val registry: LauncherRegistry,
    private val scope: CoroutineScope
) {

    init {
        registry.onPdfResult = onPdfResult@{ uri ->
            val pdfConfig = config.pdf ?: return@onPdfResult

            handleDocumentResult(
                uri = uri,
                copyToCache = true,
                prefix = "PDF_",
                extension = { ".pdf" },
                onFailure = pdfConfig.onFailure,
                onResult = pdfConfig.onResult
            )
        }

        registry.onDocumentResult = onDocumentResult@{ uri ->
            val docConfig = config.document ?: return@onDocumentResult

            handleDocumentResult(
                uri = uri,
                copyToCache = docConfig.copyToCache,
                prefix = "DOC_",
                extension = {
                    FileHelper.extensionFromUri(context, it)
                },
                onFailure = docConfig.onFailure,
                onResult = docConfig.onResult
            )
        }
    }

    private fun handleDocumentResult(
        uri: Uri?,
        copyToCache: Boolean,
        prefix: String,
        extension: (Uri) -> String,
        onFailure: ((ShadeError) -> Unit)?,
        onResult: ((ShadeResult.Single) -> Unit)?
    ) {
        scope.launch {
            if (uri == null) {
                onFailure?.invoke(ShadeError.PickCancelled)
                return@launch
            }

            val file = if (copyToCache) {
                FileHelper.copyUriToCache(
                    context,
                    uri,
                    prefix,
                    extension(uri)
                )
            } else null

            if (copyToCache && file == null) {
                onFailure?.invoke(ShadeError.FileSaveFailed)
                return@launch
            }

            val finalUri =
                if (file != null) {
                    FileHelper.getUriFromFile(context, file)
                } else {
                    uri
                }

            onResult?.invoke(
                ShadeResult.Single(
                    uri = finalUri,
                    file = file
                )
            )
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