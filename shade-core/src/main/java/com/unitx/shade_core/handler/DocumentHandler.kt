package com.unitx.shade_core.handler

import android.content.Context
import android.net.Uri
import com.unitx.shade_core.common.DocumentMimeType
import com.unitx.shade_core.common.FileHelper
import com.unitx.shade_core.common.action.ShadeAction
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
        registry.onDocumentResult = onDocumentResult@{ uri ->
            val docConfig = config.document ?: return@onDocumentResult

            handleDocumentResult(
                uri = uri,
                cacheConfig = docConfig.copyToCache,
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
        cacheConfig: CacheConfig?,
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

            val file = if (cacheConfig?.enabled == true) {
                FileHelper.copyUriToCache(
                    context = context,
                    uri = uri,
                    prefix = prefix,
                    extension = extension(uri),
                    onProgress = cacheConfig.onProgress
                )
            } else null

            if (cacheConfig?.enabled == true && file == null) {
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

    fun handleDocument(action: ShadeAction.Document) {
        config.document ?: return
        val mimeTypes = action.mimeTypes
            .map { it.value }
            .takeIf { it.isNotEmpty() }
            ?.toTypedArray()
            ?: DocumentMimeType.ALL_VALUE_TYPED_ARRAY
        registry.documentPickerLauncher.launch(mimeTypes)
    }
}