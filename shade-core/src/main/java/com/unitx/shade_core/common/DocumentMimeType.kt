package com.unitx.shade_core.common

/**
 * MIME types supported by the Shade document picker.
 *
 * Pass a list to [ShadeAction.Document] to restrict the picker to specific formats.
 * Defaults to all types when none are specified.
 *
 * ```kotlin
 * // All supported document types (default)
 * shade.launch(ShadeAction.Document())
 *
 * // PDF only
 * shade.launch(ShadeAction.Document(listOf(DocumentMimeType.PDF)))
 *
 * // Word documents only
 * shade.launch(ShadeAction.Document(listOf(DocumentMimeType.DOC, DocumentMimeType.DOCX)))
 * ```
 */
enum class DocumentMimeType(val value: String) {

    /** Legacy Word document (.doc) */
    DOC("application/msword"),

    /** Modern Word document (.docx) */
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),

    /** Legacy Excel spreadsheet (.xls) */
    XLS("application/vnd.ms-excel"),

    /** Modern Excel spreadsheet (.xlsx) */
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),

    /** Legacy PowerPoint presentation (.ppt) */
    PPT("application/vnd.ms-powerpoint"),

    /** Modern PowerPoint presentation (.pptx) */
    PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation"),

    /** Plain text file (.txt) */
    TXT("text/plain"),

    /** Comma-separated values (.csv) */
    CSV("text/csv"),

    /** Portable Document Format (.pdf) */
    PDF("application/pdf"),

    /** Rich Text Format (.rtf) */
    RTF("application/rtf");

    internal companion object {
        val ALL_VALUE_TYPED_ARRAY: Array<String> = entries.map { it.value }.toTypedArray()
        val ALL_ENTRY_LIST: List<DocumentMimeType> = entries as List<DocumentMimeType>
    }
}