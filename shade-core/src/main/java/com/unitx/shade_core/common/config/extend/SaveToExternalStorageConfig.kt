package com.unitx.shade_core.common.config.extend

import com.unitx.shade_core.interop.JavaStringSupplier
import java.io.File

/**
 * Configuration for saving captured files to external storage.
 *
 * When enabled, the final file (compressed or raw) is written to [path]
 * and the cache copy is deleted automatically.
 *
 * [fileNameProvider] is evaluated lazily at save time — not when this
 * config block runs — so it can safely reference values that aren't
 * ready yet at setup time (e.g. fields populated later in `onCreate`).
 *
 * ```kotlin
 * saveToExternalStorage {
 *     enabled = true
 *     path = File(context.getExternalFilesDir(null), "MyApp")
 *     fileNameProvider = { "${jobId}_photo.jpg" }
 * }
 * ```
 *
 * ```java
 * cameraConfig.saveToExternalStorage(externalStorageConfig -> {
 *     externalStorageConfig.setEnabled(true);
 *     externalStorageConfig.setPath(new File(context.getExternalFilesDir(null), "MyApp"));
 *     externalStorageConfig.setFileNameProvider(() -> jobId + "_photo.jpg");
 * });
 * ```
 *
 * @see com.unitx.shade_core.common.config.base.CameraConfig
 */
class SaveToExternalStorageConfig {
    /** Whether external storage saving is enabled. */
    var enabled: Boolean = false

    /** Target directory where the file will be saved. Created automatically if it does not exist. */
    var path: File? = null

    /**
     * Resolved file name, invoked from [fileNameProvider] at save time.
     * `null` if no provider was set — callers (e.g. [FileHelper]) should
     * fall back to the original captured file's name in that case.
     */
    internal val fileName: String? get() = fileNameProvider?.invoke()

    /**
     * Optional file name (including extension) to use when saving.
     * Evaluated lazily at save time. For a fixed name, just return
     * a constant: `fileNameProvider = { "name.jpg" }`.
     */
    var fileNameProvider: (() -> String)? = null
        @JvmName("setFileNameProviderKt") set

    /**
     * Java-friendly setter for [fileNameProvider].
     * Avoids requiring Java callers to implement a Kotlin `Function0<String>`.
     */
    fun setFileNameProvider(supplier: JavaStringSupplier?) {
        fileNameProvider = supplier?.let { { it.get() } }
    }
}