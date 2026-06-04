package com.unitx.shade_core.common.processor

import android.content.Context
import android.net.Uri
import com.unitx.shade_core.common.FileHelper
import com.unitx.shade_core.common.compressor.VideoCompressor
import com.unitx.shade_core.common.config.extend.CacheConfig
import com.unitx.shade_core.common.config.extend.CompressionConfig
import com.unitx.shade_core.common.result.ShadeCompressionException
import com.unitx.shade_core.common.result.ShadeFileSaveException
import com.unitx.shade_core.common.result.ShadeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileHandle
import java.io.File
import java.io.FileOutputStream

internal object VideoProcessor {

    suspend fun process(
        context: Context,
        uris: List<Uri>,
        files: List<File?>? = null,
        prefix: String,
        extension: String,
        copyToCache: CacheConfig?,
        compression: CompressionConfig?,
        authority: String
    ): List<ShadeResult.ShadeMedia> = withContext(Dispatchers.IO) {

        val processedFiles = if (compression?.enabled == true) {
            compressFromUri(
                context = context,
                uris = uris,
                prefix = prefix,
                extension = extension,
                compression = compression
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
            val finalUri = when {
                compression?.enabled == true && processedFile != null ->
                    FileHelper.getUriFromFile(context = context, file = processedFile, authority = authority)
                files == null && processedFile != null ->
                    FileHelper.getUriFromFile(context = context, file = processedFile, authority = authority)
                else -> originalUri
            }
            ShadeResult.ShadeMedia(uri = finalUri, file = processedFile)
        } ?: uris.map { uri ->
            ShadeResult.ShadeMedia(uri = uri, file = null)
        }
    }

    suspend fun process(
        context: Context,
        uri: Uri,
        file: File? = null,
        prefix: String,
        extension: String,
        copyToCache: CacheConfig?,
        compression: CompressionConfig?,
        authority: String
    ): ShadeResult.ShadeMedia = withContext(Dispatchers.IO) {

        val processedFile = if (compression?.enabled == true) {
            compressFromUri(
                context = context,
                uri = uri,
                prefix = prefix,
                extension = extension,
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

        val finalUri = when {
            compression?.enabled == true && processedFile != null ->
                FileHelper.getUriFromFile(context = context, file = processedFile, authority = authority)
            file == null && processedFile != null ->
                FileHelper.getUriFromFile(context = context, file = processedFile, authority = authority)
            else -> uri
        }

        return@withContext ShadeResult.ShadeMedia(uri = finalUri, file = processedFile)
    }

    private suspend fun compressFromUri(
        context: Context,
        uri: Uri,
        prefix: String,
        extension: String,
        compression: CompressionConfig
    ): File {
        val sourceFile = withContext(Dispatchers.IO) {
            File.createTempFile("${prefix}SRC_", extension, FileHelper.shadeCacheDir(context))
        }

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(sourceFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw ShadeCompressionException(
                cause = IllegalStateException("openInputStream returned null for uri: $uri")
            )

            val result = VideoCompressor.compress(
                context = context,
                input = sourceFile,
                params = VideoCompressor.CompressionParams(
                    videoBitrate = compression.videoBitrate,
                    frameRate = compression.frameRate,
                    maxWidth = compression.maxWidth,
                    maxHeight = compression.maxHeight,
                    keyFrameInterval = compression.keyFrameInterval
                ),
                onProgress = compression.onProgress
            )

            return result.getOrElse { cause -> throw ShadeCompressionException(cause) }

        } finally {
            sourceFile.delete()
        }
    }

    private suspend fun compressFromUri(
        context: Context,
        uris: List<Uri>,
        prefix: String,
        extension: String,
        compression: CompressionConfig
    ): List<File?> = withContext(Dispatchers.IO) {

        val sourceFiles = uris.map { uri ->
            val sourceFile = File.createTempFile("${prefix}SRC_", extension, FileHelper.shadeCacheDir(context))
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(sourceFile).use { input.copyTo(it) }
            } ?: throw ShadeCompressionException(
                cause = IllegalStateException("openInputStream returned null for uri: $uri")
            )
            sourceFile
        }

        try {
            val results = VideoCompressor.compress(
                context = context,
                inputs = sourceFiles,
                params = VideoCompressor.CompressionParams(
                    videoBitrate = compression.videoBitrate,
                    frameRate = compression.frameRate,
                    maxWidth = compression.maxWidth,
                    maxHeight = compression.maxHeight,
                    keyFrameInterval = compression.keyFrameInterval
                ),
                onProgress = compression.onProgress
            )

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