package com.unitx.shade_core.compose

/** Mutable callback container — avoids capturing stale lambdas across recompositions. */
internal class PermissionCallbackHolder {
    var onCamera: ((Boolean) -> Unit)? = null
    var onMedia: ((Boolean) -> Unit)? = null
}