package com.unitx.shade_core.common

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
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
        withContext(Dispatchers.IO) {
            try {
                val file = File.createTempFile(prefix, ext, context.cacheDir)
                val uri = getUriFromFile(context, file)
                Pair(file, uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalStateException) {
                throw e
            }  catch (_: Exception){
                null
            }
        }

    internal suspend fun copyUriToCache(
        context: Context,
        uri: Uri,
        prefix: String,
        extension: String
    ): File? = withContext(Dispatchers.IO) {
        try {
            val file = File.createTempFile(prefix, extension, context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            file
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
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
}