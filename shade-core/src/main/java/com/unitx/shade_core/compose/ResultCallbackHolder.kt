package com.unitx.shade_core.compose

internal class ResultCallbackHolder<T> {
    var callback: ((T) -> Unit)? = null
}
