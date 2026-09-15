package com.unitx.shade_core.interop

/**
 * Java-friendly functional interface for lazily supplying a file name.
 *
 * Needed because Java doesn't have Kotlin's `() -> String` lambda syntax;
 * this allows Java callers to write `setFileNameProvider(() -> "name.jpg")`
 * without dealing with `kotlin.jvm.functions.Function0` directly.
 */
fun interface JavaStringSupplier {
    fun get(): String
}