package com.unitx.shade_core.common.action

import com.unitx.shade_core.common.DocumentMimeType

/**
 * Represents a specific media or file action to be performed by Shade.
 *
 * This sealed class hierarchy defines all supported operations, such as capturing
 * media via camera, picking from a gallery, or selecting documents. Pass an instance
 * of [ShadeAction] to `ShadeCore.launch()` to initiate the flow.
 */
sealed class ShadeAction {

    sealed class Image : ShadeAction() {
        object Camera : Image()
        object Gallery : Image()
    }

    // ─── Video ────────────────────────────────────────────────────────────────

    sealed class Video : ShadeAction() {
        object Camera : Video()
        object Gallery : Video()
    }

    data class Document(val mimeTypes: List<DocumentMimeType> = DocumentMimeType.ALL_ENTRY_LIST) : ShadeAction()
}