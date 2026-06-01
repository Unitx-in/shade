package com.unitx.shade_core.common.processor

import android.content.Context
import android.net.Uri
import com.unitx.shade_core.common.FileHelper
import com.unitx.shade_core.common.config.extend.CacheConfig
import com.unitx.shade_core.common.result.ShadeFileSaveException
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
            ) ?: throw ShadeFileSaveException(uri = uri)
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

        if (copyToCache?.enabled != true) {
            return@withContext uris.map { uri -> ShadeResult.ShadeMedia(uri = uri, file = null) }
        }

        val files = FileHelper.copyUriToCache(
            context = context,
            uris = uris,
            prefix = prefix,
            extensions = extensions,
            onProgress = copyToCache.onProgress
        )

        val failedUris = files.mapIndexedNotNull { i, f -> if (f == null) uris.getOrNull(i) else null }
        if (failedUris.isNotEmpty()) throw ShadeFileSaveException(uris = failedUris)

        // All files are non-null here — throw above guarantees it
        files.mapIndexed { index, file ->
            val savedFile = file!!
            val finalUri = FileHelper.getUriFromFile(context, savedFile)
            ShadeResult.ShadeMedia(uri = finalUri, file = savedFile)
        }
    }
}