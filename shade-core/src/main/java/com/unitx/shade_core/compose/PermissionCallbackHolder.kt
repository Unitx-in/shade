package com.unitx.shade_core.compose

internal class PermissionCallbackHolder {
    var onCamera: ((Boolean) -> Unit)? = null
    var onMedia: ((Boolean) -> Unit)? = null
}