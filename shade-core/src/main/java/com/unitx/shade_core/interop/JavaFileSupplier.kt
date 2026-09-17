package com.unitx.shade_core.interop

import java.io.File

/**
 * Java-friendly functional interface for lazily supplying a [File].
 *
 * Kotlin callers use a `() -> File` lambda directly; Java callers implement
 * this interface instead, since Java has no clean way to satisfy a Kotlin
 * `Function0<File>` from a lambda without extra ceremony.
 *
 * ```java
 * externalStorageConfig.setPathProvider(() -> new File(context.getExternalFilesDir(null), jobId));
 * ```
 */
fun interface JavaFileSupplier {
    fun get(): File
}