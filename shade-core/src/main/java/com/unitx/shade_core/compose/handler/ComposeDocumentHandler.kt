package com.unitx.shade_core.compose.handler

import androidx.activity.result.ActivityResultLauncher
import com.unitx.shade_core.common.action.ShadeAction
import com.unitx.shade_core.common.DocumentMimeType
import com.unitx.shade_core.compose.state.ShadeResultHolder
import com.unitx.shade_core.common.config.ShadeConfig
import com.unitx.shade_core.common.result.ShadeResult
import android.content.Context
import com.unitx.shade_core.common.FileHelper
import com.unitx.shade_core.common.result.ShadeError
import com.unitx.shade_core.common.processor.DocumentProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.unitx.shade_core.compose.core.ComposeShadeCore

/**
 * Compose-side document handler.
 *
 * Wires single and multi document result callbacks and exposes
 * [handleDocument] to [ComposeShadeCore].
 */
internal class ComposeDocumentHandler(
    private val context: Context,
    private val config: ShadeConfig,
    private val documentSingleLauncher: ActivityResultLauncher<Array<String>>?,
    private val documentMultiLauncher: ActivityResultLauncher<Array<String>>?,
    documentSingleCallback: ShadeResultHolder,
    documentMultiCallback: ShadeResultHolder,
    private val scope: CoroutineScope,
) {

    init {

        // ── Document single ────────────────────────────────────────────────────

        documentSingleCallback.onResult = onResult@{ result ->

            val single = result as ShadeResult.Single
            val documentConfig = config.document ?: return@onResult

            scope.launch {

                val processed = DocumentProcessor.process(
                    context = context,
                    uri = single.uri,
                    prefix = "DOC_",
                    extension = FileHelper.extensionFromUri(context, single.uri),
                    copyToCache = documentConfig.copyToCache,
                )

                if (documentConfig.copyToCache?.enabled == true && processed.file == null) {
                    documentConfig.onFailure?.invoke(ShadeError.FileSaveFailed)
                    return@launch
                }

                documentConfig.onResult?.invoke(
                    ShadeResult.Single(
                        uri = processed.uri,
                        file = processed.file
                    )
                )
            }
        }

        documentSingleCallback.onFailure = {
            config.document?.onFailure?.invoke(it)
        }

        // ── Document multi ─────────────────────────────────────────────────────

        documentMultiCallback.onResult = onResult@{ result ->

            val multiple = result as ShadeResult.Multiple
            val documentConfig = config.document ?: return@onResult

            scope.launch {

                val items = DocumentProcessor.process(
                    context = context,
                    uris = multiple.items.map { it.uri },
                    prefix = "DOC_",
                    extensions = multiple.items.map {
                        FileHelper.extensionFromUri(context, it.uri)
                    },
                    copyToCache = documentConfig.copyToCache,
                )

                if (documentConfig.copyToCache?.enabled == true && items.any { it.file == null }) {
                    documentConfig.onFailure?.invoke(ShadeError.FileSaveFailed)
                    return@launch
                }

                documentConfig.onResult?.invoke(
                    ShadeResult.Multiple(items)
                )
            }
        }

        documentMultiCallback.onFailure = {
            config.document?.onFailure?.invoke(it)
        }
    }

    fun handleDocument(action: ShadeAction.Document) {
        val documentConfig = config.document ?: return
        val mimes = action.mimeTypes
            .map { it.value }
            .takeIf { it.isNotEmpty() }
            ?.toTypedArray()
            ?: DocumentMimeType.ALL_VALUE_TYPED_ARRAY

        if (documentConfig.multiSelect?.enabled == true)
            documentMultiLauncher?.launch(mimes)
        else
            documentSingleLauncher?.launch(mimes)
    }
}