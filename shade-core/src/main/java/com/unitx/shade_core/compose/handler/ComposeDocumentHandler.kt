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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Compose-side document handler.
 *
 * Wires PDF and document result callbacks and exposes
 * [handlePdf] / [handleDocument] to [ComposeShadeCore].
 */
internal class ComposeDocumentHandler(
    private val context: Context,
    private val config: ShadeConfig,
    private val pdfLauncher: ActivityResultLauncher<Array<String>>?,
    private val documentLauncher: ActivityResultLauncher<Array<String>>?,
    pdfCallback: ShadeResultHolder,
    documentCallback: ShadeResultHolder,
    private val scope: CoroutineScope,
) {

    init {

        // ── PDF result ─────────────────────────────────────────────────────────

        pdfCallback.onResult = onResult@{ result ->

            val single = result as ShadeResult.Single
            val pdfConfig = config.pdf ?: return@onResult

            scope.launch {

                val file = FileHelper.copyUriToCache(
                    context = context,
                    uri = single.uri,
                    prefix = "PDF_",
                    extension = ".pdf"
                )

                if (file == null) {
                    pdfConfig.onFailure?.invoke(ShadeError.FileSaveFailed)
                    return@launch
                }

                pdfConfig.onResult?.invoke(
                    ShadeResult.Single(
                        uri = single.uri,
                        file = file
                    )
                )
            }
        }

        pdfCallback.onFailure = {
            config.pdf?.onFailure?.invoke(it)
        }

        // ── Document result ────────────────────────────────────────────────────

        documentCallback.onResult = onResult@{ result ->

            val single = result as ShadeResult.Single
            val documentConfig = config.document ?: return@onResult

            scope.launch {

                val file =
                    if (documentConfig.copyToCache) {

                        FileHelper.copyUriToCache(
                            context = context,
                            uri = single.uri,
                            prefix = "DOC_",
                            extension = FileHelper.extensionFromUri(
                                context,
                                single.uri
                            )
                        )

                    } else null

                if (documentConfig.copyToCache && file == null) {
                    documentConfig.onFailure?.invoke(ShadeError.FileSaveFailed)
                    return@launch
                }

                documentConfig.onResult?.invoke(
                    ShadeResult.Single(
                        uri = single.uri,
                        file = file
                    )
                )
            }
        }

        documentCallback.onFailure = {
            config.document?.onFailure?.invoke(it)
        }
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