package com.unitx.shade_core.handler

import android.content.Context
import android.net.Uri
import com.unitx.shade_core.common.DocumentMimeType
import com.unitx.shade_core.common.FileHelper
import com.unitx.shade_core.common.action.ShadeAction
import com.unitx.shade_core.common.processor.DocumentProcessor
import com.unitx.shade_core.common.config.ShadeConfig
import com.unitx.shade_core.common.config.extend.CacheConfig
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
        registry.onDocumentSingleResult = onDocumentSingleResult@{ uri ->
            val docConfig = config.document ?: return@onDocumentSingleResult

            scope.launch {
                if (uri == null) {
                    docConfig.onFailure?.invoke(ShadeError.PickCancelled)
                    return@launch
                }

                val processed = DocumentProcessor.process(
                    context = context,
                    uri = uri,
                    prefix = "DOC_",
                    extension = FileHelper.extensionFromUri(context, uri),
                    copyToCache = docConfig.copyToCache,
                )

                if (docConfig.copyToCache?.enabled == true && processed.file == null) {
                    docConfig.onFailure?.invoke(ShadeError.FileSaveFailed)
                    return@launch
                }

                docConfig.onResult?.invoke(
                    ShadeResult.Single(uri = processed.uri, file = processed.file)
                )
            }
        }

        registry.onDocumentMultiResult = onDocumentMultiResult@{ uris ->
            val docConfig = config.document ?: return@onDocumentMultiResult

            scope.launch {
                if (uris.isEmpty()) {
                    docConfig.onFailure?.invoke(ShadeError.PickCancelled)
                    return@launch
                }

                val items = DocumentProcessor.process(
                    context = context,
                    uris = uris,
                    prefix = "DOC_",
                    extensions = uris.map { FileHelper.extensionFromUri(context, it) },
                    copyToCache = docConfig.copyToCache,
                )

                if (docConfig.copyToCache?.enabled == true && items.any { it.file == null }) {
                    docConfig.onFailure?.invoke(ShadeError.FileSaveFailed)
                    return@launch
                }

                docConfig.onResult?.invoke(ShadeResult.Multiple(items))
            }
        }
    }

    fun handleDocument(action: ShadeAction.Document) {
        val docConfig = config.document ?: return
        val mimeTypes = action.mimeTypes
            .map { it.value }
            .takeIf { it.isNotEmpty() }
            ?.toTypedArray()
            ?: DocumentMimeType.ALL_VALUE_TYPED_ARRAY

        if (docConfig.multiSelect?.enabled == true) registry.documentMultiLauncher.launch(mimeTypes)
        else registry.documentSingleLauncher.launch(mimeTypes)
    }
}