package com.unitx.shade_core.common.okHttp

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull

enum class OkHttpMimeType(val value: String, val extensions: List<String>) {
    // Images
    JPEG("image/jpeg", listOf("jpg", "jpeg")),
    PNG("image/png", listOf("png")),
    GIF("image/gif", listOf("gif")),
    WEBP("image/webp", listOf("webp")),

    // Videos
    MP4("video/mp4", listOf("mp4")),
    MOV("video/quicktime", listOf("mov")),
    AVI("video/x-msvideo", listOf("avi")),
    MKV("video/x-matroska", listOf("mkv")),
    GP3("video/3gpp", listOf("3gp")),

    // Documents
    PDF("application/pdf", listOf("pdf")),
    DOC("application/msword", listOf("doc")),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", listOf("docx")),
    XLS("application/vnd.ms-excel", listOf("xls")),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", listOf("xlsx")),
    PPT("application/vnd.ms-powerpoint", listOf("ppt")),
    PPTX(
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        listOf("pptx")
    ),
    TXT("text/plain", listOf("txt")),
    CSV("text/csv", listOf("csv")),
    RTF("application/rtf", listOf("rtf")),

    // Fallback
    OCTET_STREAM("application/octet-stream", emptyList());

    fun toMediaType(): MediaType = value.toMediaTypeOrNull()!!

    companion object {
        fun fromExtension(ext: String): OkHttpMimeType =
            entries.firstOrNull { ext.lowercase() in it.extensions } ?: OCTET_STREAM
    }
}