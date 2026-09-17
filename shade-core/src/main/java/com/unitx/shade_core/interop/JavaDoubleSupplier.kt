package com.unitx.shade_core.interop

/**
 * Java-friendly functional interface for lazily supplying a [Double].
 *
 * ```java
 * compressionConfig.setMaxFileSizeKbProvider(() -> uploadLimitKb);
 * ```
 */
fun interface JavaDoubleSupplier {
    fun get(): Double
}