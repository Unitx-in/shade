package com.unitx.shade_core.common.processor

import android.content.Context
import android.net.Uri
import com.unitx.shade_core.common.FileHelper
import com.unitx.shade_core.common.compressor.ImageCompressor
import com.unitx.shade_core.common.config.extend.CacheConfig
import com.unitx.shade_core.common.config.extend.CompressionConfig
import com.unitx.shade_core.common.config.extend.ProgressConfig
import com.unitx.shade_core.common.result.ShadeCompressionException
import com.unitx.shade_core.common.result.ShadeFileSaveException
import com.unitx.shade_core.common.result.ShadeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

internal object ImageProcessor {

    suspend fun process(
        context: Context,
        uri: Uri,
        file: File? = null,
        prefix: String,
        extension: String,
        copyToCache: CacheConfig?,
        compression: CompressionConfig?,
    ): ShadeResult.ShadeMedia = withContext(Dispatchers.IO) {

        val processedFile = if (compression?.enabled == true) {
            compressFromUri(
                context = context,
                uri = uri,
                prefix = prefix,
                compression = compression
            )
        } else file ?: if (copyToCache?.enabled == true) {
            FileHelper.copyUriToCache(
                context = context,
                uri = uri,
                prefix = prefix,
                extension = extension,
                onProgress = copyToCache.onProgress
            ) ?: throw ShadeFileSaveException(uri = uri)
        } else null

        val finalUri =
            if (processedFile != null && file == null) {
                FileHelper.getUriFromFile(context, processedFile)
            } else {
                uri
            }

        return@withContext ShadeResult.ShadeMedia(
            uri = finalUri,
            file = processedFile
        )
    }

    suspend fun process(
        context: Context,
        uris: List<Uri>,
        files: List<File?>? = null,
        prefix: String,
        extension: String,
        copyToCache: CacheConfig?,
        compression: CompressionConfig?,
    ): List<ShadeResult.ShadeMedia> = withContext(Dispatchers.IO) {

        val processedFiles = if (compression?.enabled == true) {
            compressFromUri(
                context = context,
                uris = uris,
                prefix = prefix,
                compression = compression,
            )
        } else files ?: if (copyToCache?.enabled == true) {
            val copiedFiles = FileHelper.copyUriToCache(
                context = context,
                uris = uris,
                prefix = prefix,
                extension = extension,
                onProgress = copyToCache.onProgress
            )
            val failedUris = copiedFiles.mapIndexedNotNull { i, f -> if (f == null) uris.getOrNull(i) else null }
            if (failedUris.isNotEmpty()) throw ShadeFileSaveException(uris = failedUris)
            copiedFiles
        } else null

        return@withContext processedFiles?.mapIndexed { index, processedFile ->
            val originalUri = uris[index]
            val finalUri = if (processedFile != null && files == null) {
                FileHelper.getUriFromFile(context, processedFile)
            } else {
                originalUri
            }

            ShadeResult.ShadeMedia(
                uri = finalUri,
                file = processedFile
            )
        } ?: uris.map { uri ->
            ShadeResult.ShadeMedia(
                uri = uri,
                file = null
            )
        }
    }

    // Single URI version:
    private suspend fun compressFromUri(
        context: Context,
        uri: Uri,
        prefix: String,
        compression: CompressionConfig,
    ): File? {
        val sourceFile = withContext(Dispatchers.IO) {
            File.createTempFile("${prefix}SRC_", ".tmp", context.cacheDir)
        }

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(sourceFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw ShadeCompressionException(
                cause = IllegalStateException("openInputStream returned null for uri: $uri")
            )

            // Unwrap Result — throw ShadeCompressionException on failure
            val result = ImageCompressor.compress(
                context = context,
                input = sourceFile,
                quality = compression.quality,
                maxWidth = compression.maxWidth,
                maxHeight = compression.maxHeight,
                outputFormat = compression.format,
                onProgress = compression.onProgress
            )

            return result.getOrElse { cause ->
                throw ShadeCompressionException(cause)
            }

        } finally {
            sourceFile.delete()
        }
    }

    // Multi URI version:
    private suspend fun compressFromUri(
        context: Context,
        uris: List<Uri>,
        prefix: String,
        compression: CompressionConfig,
    ): List<File?> = withContext(Dispatchers.IO) {

        val sourceFiles = uris.map { uri ->
            val sourceFile = File.createTempFile("${prefix}SRC_", ".tmp", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(sourceFile).use { input.copyTo(it) }
            } ?: throw ShadeCompressionException(
                cause = IllegalStateException("openInputStream returned null for uri: $uri")
            )
            sourceFile
        }

        try {
            val results = ImageCompressor.compress(
                context = context,
                inputs = sourceFiles,
                quality = compression.quality,
                maxWidth = compression.maxWidth,
                maxHeight = compression.maxHeight,
                onProgress = compression.onProgress,
                outputFormat = compression.format,
            )

            // Map each Result — collect failures, throw if any
            val failures = results.mapIndexedNotNull { index, result ->
                result.exceptionOrNull()?.let { index to it }
            }

            if (failures.isNotEmpty()) {
                throw ShadeCompressionException(
                    cause = failures.first().second,
                    failedIndices = failures.map { it.first }
                )
            }

            results.map { it.getOrNull() }

        } finally {
            sourceFiles.forEach { it.delete() }
        }
    }
}