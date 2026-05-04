package com.unitx.shade_core.action


sealed class ShadeAction {

    // ─── Image ────────────────────────────────────────────────────────────────

    sealed class Image : ShadeAction() {
        object Camera : Image()
        object Gallery : Image()
    }

    // ─── Video ────────────────────────────────────────────────────────────────

    sealed class Video : ShadeAction() {
        object Camera : Video()
        object Gallery : Video()
    }

    object Pdf : ShadeAction()

    data class Document(
        val mimeTypes: List<String> = emptyList()
    ) : ShadeAction()
}