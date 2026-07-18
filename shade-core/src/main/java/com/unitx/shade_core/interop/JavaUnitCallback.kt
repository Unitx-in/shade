package com.unitx.shade_core.interop

fun interface JavaUnitCallback<T> {
    fun invoke(value: T)
}