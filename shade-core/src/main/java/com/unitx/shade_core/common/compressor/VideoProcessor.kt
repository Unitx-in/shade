package com.unitx.shade_core.common.compressor

import android.content.Context
import android.net.Uri
import com.unitx.shade_core.common.FileHelper
import com.unitx.shade_core.common.config.extend.CompressionConfig
import com.unitx.shade_core.common.config.extend.CacheConfig
import com.unitx.shade_core.common.result.ShadeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

internal object VideoProcessor {

    suspend fun process(
        context: Context,
        uri: Uri,
        file: File? = null,
        prefix: String,
        extension: String,
        copyToCache: CacheConfig?,
        compression: CompressionConfig?,
    ): ShadeResult.ShadeMedia = withContext(Dispatchers.IO) {

        val processedFile =
            if (compression?.enabled == true) {

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
                    )

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

    private suspend fun compressFromUri(
        context: Context,
        uri: Uri,
        prefix: String,
        extension: String,
        compression: CompressionConfig
    ): File? {

        val sourceFile = withContext(Dispatchers.IO) {
            File.createTempFile(
                "${prefix}SRC_",
                extension,
                context.cacheDir
            )
        }

        try {

            context.contentResolver.openInputStream(uri)?.use { input ->

                FileOutputStream(sourceFile).use { output ->
                    input.copyTo(output)
                }

            } ?: return null

            return VideoCompressor.compress(
                input = sourceFile,
                params = VideoCompressor.CompressionParams(
                    videoBitrate = compression.videoBitrate,
                    frameRate = compression.frameRate,
                    maxWidth = compression.maxWidth,
                    maxHeight = compression.maxHeight,
                    keyFrameInterval = compression.keyFrameInterval
                )
            )

        } finally {

            sourceFile.delete()
        }
    }
}