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
import com.unitx.shade_core.compose.core.ComposeShadeCore

/**
 * Compose-side document handler.
 *
 * Wires PDF and document result callbacks and exposes
 * [handlePdf] / [handleDocument] to [ComposeShadeCore].
 */
internal class ComposeDocumentHandler(
    private val context: Context,
    private val config: ShadeConfig,
    private val documentLauncher: ActivityResultLauncher<Array<String>>?,
    documentCallback: ShadeResultHolder,
    private val scope: CoroutineScope,
) {

    init {

        // ── Document result ────────────────────────────────────────────────────

        documentCallback.onResult = onResult@{ result ->

            val single = result as ShadeResult.Single
            val documentConfig = config.document ?: return@onResult

            scope.launch {

                val file = if (documentConfig.copyToCache?.enabled == true) {

                        FileHelper.copyUriToCache(
                            context = context,
                            uri = single.uri,
                            prefix = "DOC_",
                            extension = FileHelper.extensionFromUri(
                                context,
                                single.uri
                            ),
                            onProgress = documentConfig.copyToCache?.onProgress
                        )

                    } else null

                if (documentConfig.copyToCache?.enabled == true && file == null) {
                    documentConfig.onFailure?.invoke(ShadeError.FileSaveFailed)
                    return@launch
                }

                val finalUri =
                    if (file != null) {
                        FileHelper.getUriFromFile(context, file)
                    } else {
                        single.uri
                    }

                documentConfig.onResult?.invoke(
                    ShadeResult.Single(
                        uri = finalUri,
                        file = file
                    )
                )
            }
        }

        documentCallback.onFailure = {
            config.document?.onFailure?.invoke(it)
        }
    }

    fun handleDocument(action: ShadeAction.Document) {
        config.document ?: return
        val mimes = action.mimeTypes
            .map { it.value }
            .takeIf { it.isNotEmpty() }
            ?.toTypedArray()
            ?: DocumentMimeType.ALL_VALUE_TYPED_ARRAY
        documentLauncher?.launch(mimes)
    }
}