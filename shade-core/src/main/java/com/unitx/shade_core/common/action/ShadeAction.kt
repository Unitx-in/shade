package com.unitx.shade_core.common.action

import com.unitx.shade_core.common.DocumentMimeType

/**
 * Represents the set of actions that can be launched via [ShadeCore.launch].
 *
 * Actions are grouped by media type. Each action maps to a specific system
 * UI flow — camera capture, gallery picker, or document picker.
 *
 * ## Usage
 * ```kotlin
 * shade.launch(ShadeAction.Image.Camera)
 * shade.launch(ShadeAction.Video.Gallery)
 * shade.launch(ShadeAction.Document())
 * shade.launch(ShadeAction.Document(listOf(DocumentMimeType.PDF)))
 * ```
 */
sealed class ShadeAction {

    /**
     * Actions for image capture and selection.
     */
    sealed class Image : ShadeAction() {

        /**
         * Opens the device camera to capture a photo.
         *
         * Requires camera permission. If not granted, Shade will request it
         * automatically and invoke `onFailure` with [ShadeError.PermissionDenied]
         * or [ShadeError.PermissionPermanentlyDenied] if refused.
         */
        object Camera : Image()

        /**
         * Opens the system photo picker to select an image.
         *
         * Supports single and multi-select depending on whether
         * `multiSelect` was configured in the `gallery { }` block.
         * Result is delivered as [ShadeResult.Single] or [ShadeResult.Multiple].
         */
        object Gallery : Image()
    }

    /**
     * Actions for video capture and selection.
     */
    sealed class Video : ShadeAction() {

        /**
         * Opens the device camera to record a video.
         *
         * Requires camera permission. If not granted, Shade will request it
         * automatically and invoke `onFailure` with [ShadeError.PermissionDenied]
         * or [ShadeError.PermissionPermanentlyDenied] if refused.
         */
        object Camera : Video()

        /**
         * Opens the system video picker to select a video.
         *
         * Supports single and multi-select depending on whether
         * `multiSelect` was configured in the `gallery { }` block.
         * On API 33+, Shade requests the required media permission automatically.
         * Result is delivered as [ShadeResult.Single] or [ShadeResult.Multiple].
         */
        object Gallery : Video()
    }

    /**
     * Opens the system document picker to select one or more documents.
     *
     * @param mimeTypes The MIME types to filter in the picker.
     * Defaults to [DocumentMimeType.ALL_ENTRY_LIST], which accepts all supported
     * document types. Pass a specific list to restrict the picker to certain formats:
     *
     * ```kotlin
     * shade.launch(ShadeAction.Document(listOf(DocumentMimeType.PDF)))
     * ```
     *
     * Supports single and multi-select depending on whether `multiSelect`
     * was configured in the `document { }` block.
     * Result is delivered as [ShadeResult.Single] or [ShadeResult.Multiple].
     */
    data class Document(
        val mimeTypes: List<DocumentMimeType> = DocumentMimeType.ALL_ENTRY_LIST
    ) : ShadeAction()
}