package com.unitx.shade_core.handler

import android.content.Context
import android.util.Log
import com.unitx.shade_core.common.DocumentMimeType
import com.unitx.shade_core.common.FileHelper
import com.unitx.shade_core.common.action.ShadeAction
import com.unitx.shade_core.common.processor.DocumentProcessor
import com.unitx.shade_core.common.config.ShadeConfig
import com.unitx.shade_core.core.LauncherRegistry
import com.unitx.shade_core.common.result.ShadeError
import com.unitx.shade_core.common.result.ShadeFileSaveException
import com.unitx.shade_core.common.result.ShadeResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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

                try {
                    val processed = DocumentProcessor.process(
                        context = context,
                        uri = uri,
                        prefix = "DOC_",
                        extension = FileHelper.extensionFromUri(context, uri),
                        copyToCache = docConfig.copyToCache,
                        authority = config.getFilesProviderAuthority()
                    )

                    docConfig.onResult?.invoke(
                        ShadeResult.Single(uri = processed.uri, file = processed.file)
                    )

                } catch (e: ShadeFileSaveException) {
                    docConfig.onFailure?.invoke(ShadeError.FileSaveFailed(uri = e.uri))
                } catch (e: CancellationException) {
                    Log.i("ShadeError", "Cancellation exception during document processing")
                    throw e
                } catch (e: Exception) {
                    docConfig.onFailure?.invoke(
                        ShadeError.DocumentProcessingFailed(
                            failedUris = listOf(uri),
                            cause = e
                        )
                    )
                }
            }
        }

        registry.onDocumentMultiResult = onDocumentMultiResult@{ uris ->
            val docConfig = config.document ?: return@onDocumentMultiResult

            scope.launch {
                if (uris.isEmpty()) {
                    docConfig.onFailure?.invoke(ShadeError.PickCancelled)
                    return@launch
                }

                try {
                    val items = DocumentProcessor.process(
                        context = context,
                        uris = uris,
                        prefix = "DOC_",
                        extensions = uris.map { FileHelper.extensionFromUri(context, it) },
                        copyToCache = docConfig.copyToCache,
                        authority = config.getFilesProviderAuthority()
                    )

                    docConfig.onResult?.invoke(ShadeResult.Multiple(items))

                } catch (e: ShadeFileSaveException) {
                    docConfig.onFailure?.invoke(ShadeError.FileSaveFailed(uris = e.uris))
                } catch (e: CancellationException) {
                    Log.i("ShadeError", "Cancellation exception during document processing")
                    throw e
                } catch (e: Exception) {
                    docConfig.onFailure?.invoke(
                        ShadeError.DocumentProcessingFailed(
                            failedUris = uris,
                            cause = e
                        )
                    )
                }
            }
        }
    }

    fun handleDocument(action: ShadeAction.Document) {
        val docConfig = config.document ?: return
        val mimeTypes = action.mimeTypes
            .map { it.mimeTypeValue }
            .takeIf { it.isNotEmpty() }
            ?.toTypedArray()
            ?: DocumentMimeType.ALL_VALUE_TYPED_ARRAY

        if (docConfig.multiSelect?.enabled == true) registry.documentMultiLauncher.launch(mimeTypes)
        else registry.documentSingleLauncher.launch(mimeTypes)
    }
}