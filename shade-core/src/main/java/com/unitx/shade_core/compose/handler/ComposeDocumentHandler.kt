package com.unitx.shade_core.compose.handler

import androidx.activity.result.ActivityResultLauncher
import com.unitx.shade_core.common.action.ShadeAction
import com.unitx.shade_core.common.DocumentMimeType
import com.unitx.shade_core.compose.state.ShadeResultHolder
import com.unitx.shade_core.common.config.ShadeConfig
import com.unitx.shade_core.common.result.ShadeResult
import android.content.Context
import android.util.Log
import com.unitx.shade_core.common.FileHelper
import com.unitx.shade_core.common.result.ShadeError
import com.unitx.shade_core.common.processor.DocumentProcessor
import com.unitx.shade_core.common.result.ShadeFileSaveException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
                try {
                    val processed = DocumentProcessor.process(
                        context = context,
                        uri = single.uri,
                        prefix = "DOC_",
                        extension = FileHelper.extensionFromUri(context, single.uri),
                        copyToCache = documentConfig.copyToCache,
                        authority = config.getFilesProviderAuthority()
                    )

                    documentConfig.onResult?.invoke(
                        ShadeResult.Single(uri = processed.uri, file = processed.file)
                    )

                } catch (e: ShadeFileSaveException) {
                    documentConfig.onFailure?.invoke(
                        ShadeError.FileSaveFailed(uri = e.uri, cause = e.cause)
                    )
                } catch (e: CancellationException) {
                    Log.i("ShadeError", "Cancellation exception during document processing")
                    throw e
                } catch (e: Exception) {
                    documentConfig.onFailure?.invoke(
                        ShadeError.DocumentProcessingFailed(
                            failedUris = listOf(single.uri),
                            cause = e
                        )
                    )
                }
            }
        }

        documentSingleCallback.onFailure = {
            config.document?.onFailure?.invoke(it)
        }

        // ── Document multi ─────────────────────────────────────────────────────

        documentMultiCallback.onResult = onResult@{ result ->

            val multiple = result as ShadeResult.Multiple
            val documentConfig = config.document ?: return@onResult
            val uris = multiple.items.map { it.uri }

            scope.launch {
                try {
                    val items = DocumentProcessor.process(
                        context = context,
                        uris = uris,
                        prefix = "DOC_",
                        extensions = uris.map { FileHelper.extensionFromUri(context, it) },
                        copyToCache = documentConfig.copyToCache,
                        authority = config.getFilesProviderAuthority()
                    )

                    documentConfig.onResult?.invoke(ShadeResult.Multiple(items))

                } catch (e: ShadeFileSaveException) {
                    documentConfig.onFailure?.invoke(
                        ShadeError.FileSaveFailed(uris = e.uris, cause = e.cause)
                    )
                } catch (e: CancellationException) {
                    Log.i("ShadeError", "Cancellation exception during document processing")
                    throw e
                } catch (e: Exception) {
                    documentConfig.onFailure?.invoke(
                        ShadeError.DocumentProcessingFailed(
                            failedUris = uris,
                            cause = e
                        )
                    )
                }
            }
        }

        documentMultiCallback.onFailure = {
            config.document?.onFailure?.invoke(it)
        }
    }

    fun handleDocument(action: ShadeAction.Document) {
        val documentConfig = config.document ?: return
        val mimes = action.mimeTypes
            .map { it.mimeTypeValue }
            .takeIf { it.isNotEmpty() }
            ?.toTypedArray()
            ?: DocumentMimeType.ALL_VALUE_TYPED_ARRAY

        if (documentConfig.multiSelect?.enabled == true)
            documentMultiLauncher?.launch(mimes)
        else
            documentSingleLauncher?.launch(mimes)
    }
}