package com.unitx.shade_core.core

/**
 * Common document MIME types.
 */
enum class DocumentMimeType(val value: String) {
    DOC("application/msword"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    XLS("application/vnd.ms-excel"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    PPT("application/vnd.ms-powerpoint"),
    PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    TXT("text/plain"),
    CSV("text/csv"),
    RTF("application/rtf");

    companion object {
        /**
         * Returns an array of all supported MIME type strings.
         */
        val ALL_VALUES: Array<String> = entries.map { it.value }.toTypedArray()
    }
}