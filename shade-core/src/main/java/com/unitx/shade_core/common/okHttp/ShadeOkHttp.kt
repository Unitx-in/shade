@file:JvmName("ShadeOkHttp")

package com.unitx.shade_core.common.okHttp

import com.unitx.shade_core.common.result.ShadeResult
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileNotFoundException

private fun File.guessMediaType(): MediaType =
    OkHttpMimeType.fromExtension(extension).toMediaType()

private fun File.requireExists(): File = apply {
    if (!exists()) throw FileNotFoundException(
        "shade-okhttp: file does not exist — $absolutePath. " +
                "Make sure copyToCache or compress is enabled in your Shade config."
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// ShadeResult.Captured  (camera — file is always non-null)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Returns an OkHttp [RequestBody] from a camera capture result.
 * MIME type is inferred from the file extension; pass [mimeType] to override.
 *
 * Requires OkHttp in your app dependencies. Compatible with Retrofit [@Body] and [@Part].
 */
fun ShadeResult.Captured.toRequestBody(
    mimeType: OkHttpMimeType? = null
): RequestBody = file.requireExists().asRequestBody(mimeType?.toMediaType() ?: file.guessMediaType())

/**
 * Returns a [MultipartBody.Part] from a camera capture result.
 * Ready for use with Retrofit [@Part] or a raw OkHttp [MultipartBody].
 *
 * Requires OkHttp in your app dependencies.
 *
 * @param name      Form-field name sent to the server (e.g. `"avatar"`).
 * @param filename  Filename in the `Content-Disposition` header. Defaults to the file name on disk.
 * @param mimeType  MIME type. Inferred from the file extension when `null`.
 */
fun ShadeResult.Captured.toMultipartPart(
    name: String,
    filename: String = file.name,
    mimeType: OkHttpMimeType? = null
): MultipartBody.Part =
    MultipartBody.Part.createFormData(name, filename, toRequestBody(mimeType))

// ─────────────────────────────────────────────────────────────────────────────
// ShadeResult.Single  (gallery / document single-pick)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Returns an OkHttp [RequestBody] from a single gallery or document pick.
 * Requires OkHttp in your app dependencies. Compatible with Retrofit [@Body] and [@Part].
 *
 * Requires `copyToCache` or `compress` to be enabled in your Shade config —
 * throws [IllegalStateException] if [ShadeResult.Single.file] is null.
 */
fun ShadeResult.Single.toRequestBody(
    mimeType: OkHttpMimeType? = null
): RequestBody {
    val f = requireNotNull(file) {
        "shade: ShadeResult.Single.file is null. Enable copyToCache or compress in your Shade config to get a File reference."
    }
    return f.requireExists().asRequestBody(mimeType?.toMediaType() ?: f.guessMediaType())
}

/**
 * Returns a [MultipartBody.Part] from a single gallery or document pick.
 * Ready for use with Retrofit [@Part] or a raw OkHttp [MultipartBody].
 * Requires OkHttp in your app dependencies.
 *
 * Requires `copyToCache` or `compress` to be enabled in your Shade config —
 * throws [IllegalStateException] if [ShadeResult.Single.file] is null.
 *
 * @param name      Form-field name (e.g. `"document"`).
 * @param filename  Defaults to the file name on disk.
 * @param mimeType  MIME type. Inferred from the file extension when `null`.
 */
fun ShadeResult.Single.toMultipartPart(
    name: String,
    filename: String? = null,
    mimeType: OkHttpMimeType? = null
): MultipartBody.Part {
    val f = requireNotNull(file) {
        "shade: ShadeResult.Single.file is null. Enable copyToCache or compress in your Shade config to get a File reference."
    }
    return MultipartBody.Part.createFormData(
        name,
        filename ?: f.name,
        toRequestBody(mimeType)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// ShadeResult.Multiple  (gallery / document multi-pick)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Returns a [RequestBody] list from a multi-pick result.
 * Requires OkHttp in your app dependencies.
 *
 * Items with a null [ShadeResult.ShadeMedia.file] are skipped —
 * enable `copyToCache` or `compress` in your Shade config to get File references.
 */
fun ShadeResult.Multiple.toRequestBodies(
    mimeType: OkHttpMimeType? = null
): List<RequestBody> = items.mapNotNull { item ->
    item.file?.let { f ->
        runCatching {
            f.requireExists().asRequestBody(mimeType?.toMediaType() ?: f.guessMediaType())
        }.getOrNull()
    }
}

/**
 * Returns a [MultipartBody.Part] list from a multi-pick result.
 * All parts share the same [name], which is the standard convention for
 * multi-file uploads (e.g. `"images"`).
 * Ready for use with Retrofit [@Part] or a raw OkHttp [MultipartBody].
 * Requires OkHttp in your app dependencies.
 *
 * Items with a null [ShadeResult.ShadeMedia.file] are skipped —
 * enable `copyToCache` or `compress` in your Shade config to get File references.
 *
 * @param name       Form-field name for all parts.
 * @param mimeType   MIME type. Inferred per file when `null`.
 * @param filenameOf Optional lambda to provide a custom filename per item. Defaults to the file name on disk.
 */
fun ShadeResult.Multiple.toMultipartParts(
    name: String,
    mimeType: OkHttpMimeType? = null,
    filenameOf: ((ShadeResult.ShadeMedia) -> String)? = null
): List<MultipartBody.Part> = items.mapNotNull { item ->
    val f = item.file ?: return@mapNotNull null
    runCatching {
        val filename = filenameOf?.invoke(item) ?: f.name
        MultipartBody.Part.createFormData(
            name,
            filename,
            f.requireExists().asRequestBody(mimeType?.toMediaType() ?: f.guessMediaType())
        )
    }.getOrNull()
}