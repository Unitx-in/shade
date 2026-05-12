package com.unitx.shade_core.common.processor

import android.content.Context
import android.net.Uri
import com.unitx.shade_core.common.FileHelper
import com.unitx.shade_core.common.config.extend.CacheConfig
import com.unitx.shade_core.common.result.ShadeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object DocumentProcessor {

    suspend fun process(
        context: Context,
        uri: Uri,
        prefix: String,
        extension: String,
        copyToCache: CacheConfig?,
    ): ShadeResult.ShadeMedia = withContext(Dispatchers.IO) {

        val processedFile = if (copyToCache?.enabled == true) {
            FileHelper.copyUriToCache(
                context = context,
                uri = uri,
                prefix = prefix,
                extension = extension,
                onProgress = copyToCache.onProgress
            )
        } else null

        val finalUri = if (processedFile != null) {
            FileHelper.getUriFromFile(context, processedFile)
        } else {
            uri
        }

        ShadeResult.ShadeMedia(uri = finalUri, file = processedFile)
    }

    suspend fun process(
        context: Context,
        uris: List<Uri>,
        prefix: String,
        extensions: List<String>,
        copyToCache: CacheConfig?,
    ): List<ShadeResult.ShadeMedia> = withContext(Dispatchers.IO) {

        val processedFiles = if (copyToCache?.enabled == true) {
            FileHelper.copyUriToCache(
                context = context,
                uris = uris,
                prefix = prefix,
                extensions = extensions,
                onProgress = copyToCache.onProgress
            )
        } else null

        processedFiles?.mapIndexed { index, processedFile ->
            val finalUri = if (processedFile != null) {
                FileHelper.getUriFromFile(context, processedFile)
            } else {
                uris[index]
            }
            ShadeResult.ShadeMedia(uri = finalUri, file = processedFile)
        } ?: uris.map { uri ->
            ShadeResult.ShadeMedia(uri = uri, file = null)
        }
    }
}