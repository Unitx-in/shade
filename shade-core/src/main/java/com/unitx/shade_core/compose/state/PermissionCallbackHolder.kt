package com.unitx.shade_core.compose.state

/**
 * Holds mutable permission callbacks wired by [ComposeCameraHandler] and
 * [ComposeGalleryHandler] after the permission launchers are registered
 * in [rememberShade].
 */
internal class PermissionCallbackHolder {
    var onCamera: ((Boolean) -> Unit)? = null
    var onMedia:  ((Boolean) -> Unit)? = null
}