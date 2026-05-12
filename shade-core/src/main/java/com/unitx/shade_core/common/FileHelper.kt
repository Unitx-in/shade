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

    internal fun getUriFromFile(
        context: Context,
        file: File
    ): Uri {
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        } catch (e: CancellationException){
            throw e
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException(
                "Shade requires a FileProvider with authority \"${context.packageName}.provider\". " +
                        "Add the following to your AndroidManifest.xml inside <application>:\n\n" +
                        "<provider\n" +
                        "    android:name=\"androidx.core.content.FileProvider\"\n" +
                        $$"    android:authorities=\"${applicationId}.provider\"\n" +
                        "    android:exported=\"false\"\n" +
                        "    android:grantUriPermissions=\"true\">\n" +
                        "    <meta-data\n" +
                        "        android:name=\"android.support.FILE_PROVIDER_PATHS\"\n" +
                        "        android:resource=\"@xml/shade_file_paths\" />\n" +
                        "</provider>",
                e
            )
        }
    }

    internal suspend fun createTempFile(
        context: Context,
        prefix: String,
        ext: String
    ): Pair<File, Uri>? =
        try {
            val file = withContext(Dispatchers.IO){ File.createTempFile(prefix, ext, context.cacheDir) }
            val uri = getUriFromFile(context, file)
            Pair(file, uri)
        } catch (e: IllegalStateException) {
            throw e
        }  catch (_: Exception){
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
            val file = File.createTempFile(prefix, extension, context.cacheDir)

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