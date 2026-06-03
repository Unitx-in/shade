package com.unitx.shade_core.common

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.unitx.shade_core.common.config.extend.ProgressConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal object FileHelper {

    internal fun shadeCacheDir(context: Context): File {
        return File(context.cacheDir, "shade_cache").also {
            if (!it.exists()) it.mkdirs()
        }
    }

    internal fun getUriFromFile(
        context: Context,
        file: File,
        authority: String
    ): Uri {
        return try {
            FileProvider.getUriForFile(
                context,
                authority,
                file
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException(
                "Shade internal error: FileProvider not configured correctly. " +
                        "Please report this at https://github.com/Unitx-in/shade/issues. " +
                        "Details: ${e.message}",
                e
            )
        }
    }

    internal suspend fun createTempFile(
        context: Context,
        prefix: String,
        ext: String,
        authority: String
    ): Pair<File, Uri>? =
        try {
            val dir = shadeCacheDir(context)
            val file = withContext(Dispatchers.IO) {
                File.createTempFile(prefix, ext, dir)
            }
            val uri = getUriFromFile(context, file, authority)
            Pair(file, uri)
        } catch (e: IllegalStateException) {
            throw e
        } catch (_: Exception) {
            null
        }

    internal fun extensionFromUri(context: Context, uri: Uri): String {
        val mime = context.contentResolver.getType(uri) ?: return ".bin"
        return when {
            mime.contains("pdf") -> ".pdf"
            mime.contains("word") -> ".doc"
            mime.contains("wordprocess") -> ".docx"
            mime.contains("ms-excel") -> ".xls"
            mime.contains("spreadsheet") -> ".xlsx"
            mime.contains("powerpoint") -> ".ppt"
            mime.contains("presentation") -> ".pptx"
            mime.contains("text/plain") -> ".txt"
            mime.contains("text/csv") -> ".csv"
            mime.contains("rtf") -> ".rtf"
            else -> ".bin"
        }
    }

    internal suspend fun copyUriToCache(
        context: Context,
        uris: List<Uri>,
        prefix: String,
        extension: String,
        onProgress: ((ProgressConfig.Copying) -> Unit)?,
    ): List<File?> = withContext(Dispatchers.IO) {

        uris.mapIndexed { index, uri ->
            copyUriToCache(
                context = context,
                uri = uri,
                prefix = prefix,
                extension = extension,
                onProgress = onProgress,
                fileNumber = index + 1
            )
        }
    }

    internal suspend fun copyUriToCache(
        context: Context,
        uris: List<Uri>,
        prefix: String,
        extensions: List<String>,
        onProgress: ((ProgressConfig.Copying) -> Unit)?,
    ): List<File?> = withContext(Dispatchers.IO) {

        uris.mapIndexed { index, uri ->
            copyUriToCache(
                context = context,
                uri = uri,
                prefix = prefix,
                extension = extensions[index],
                onProgress = onProgress,
                fileNumber = index + 1
            )
        }
    }

    internal suspend fun copyUriToCache(
        context: Context,
        uri: Uri,
        prefix: String,
        extension: String,
        onProgress: ((ProgressConfig.Copying) -> Unit)?,
        fileNumber: Int = 1,
    ): File? = withContext(Dispatchers.IO) {
        try {
            val file = File.createTempFile(prefix, extension, shadeCacheDir(context))

            val totalBytes = context.contentResolver.query(
                uri, arrayOf(OpenableColumns.SIZE), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else -1L
            } ?: -1L

            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    if (onProgress == null || totalBytes <= 0L) {
                        // No progress needed or size unknown — fast path
                        input.copyTo(output)
                    } else {
                        // Chunk-based copy with progress emission
                        val buffer = ByteArray(8192)
                        var bytesCopied = 0L
                        var lastPercent = -1
                        var bytes = input.read(buffer)
                        while (bytes >= 0) {
                            output.write(buffer, 0, bytes)
                            bytesCopied += bytes
                            val percent = ((bytesCopied * 100L) / totalBytes).toInt().coerceIn(0, 100)
                            // Only emit when percent actually changes — avoids flooding the callback
                            if (percent != lastPercent) {
                                lastPercent = percent
                                withContext(Dispatchers.Main) {
                                    onProgress.invoke(ProgressConfig.Copying(percent, fileNumber))
                                }
                            }
                            bytes = input.read(buffer)
                        }
                    }
                }
            }
            file
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
    }
}