package com.unitx.shade_core.common.config.extend

import com.unitx.shade_core.interop.JavaFileSupplier
import com.unitx.shade_core.interop.JavaStringSupplier
import java.io.File

/**
 * Configuration for saving captured files to external storage.
 *
 * When enabled, the final file (compressed or raw) is written to [path]
 * and the cache copy is deleted automatically.
 *
 * [pathProvider] and [fileNameProvider] are evaluated lazily at save time —
 * not when this config block runs — so they can safely reference values
 * that aren't ready yet at setup time (e.g. fields populated later in
 * `onCreate`).
 *
 * ```kotlin
 * saveToExternalStorage {
 *     enabled = true
 *     pathProvider = { File(context.getExternalFilesDir(null), jobId) }
 *     fileNameProvider = { "${jobId}_photo.jpg" }
 * }
 * ```
 *
 * ```java
 * cameraConfig.saveToExternalStorage(externalStorageConfig -> {
 *     externalStorageConfig.setEnabled(true);
 *     externalStorageConfig.setPathProvider(() -> new File(context.getExternalFilesDir(null), jobId));
 *     externalStorageConfig.setFileNameProvider(() -> jobId + "_photo.jpg");
 * });
 * ```
 *
 * @see com.unitx.shade_core.common.config.base.CameraConfig
 */
class SaveToExternalStorageConfig {

    internal val path: File? get() = pathProvider?.invoke()
    internal val fileName: String? get() = fileNameProvider?.invoke()

    /** Whether external storage saving is enabled. */
    var enabled: Boolean = false

    /**
     * Target directory where the file will be saved. Evaluated lazily at
     * save time. Created automatically if it does not exist.
     */
    var pathProvider: (() -> File)? = null
        @JvmName("setPathProviderKt") set

    /**
     * Optional file name (including extension) to use when saving.
     * Evaluated lazily at save time. For a fixed name, just return
     * a constant: `fileNameProvider = { "name.jpg" }`.
     */
    var fileNameProvider: (() -> String)? = null
        @JvmName("setFileNameProviderKt") set

    /**
     * Java-friendly setter for [pathProvider].
     * Avoids requiring Java callers to implement a Kotlin `Function0<File>`.
     */
    fun setPathProvider(supplier: JavaFileSupplier?) {
        pathProvider = supplier?.let { { it.get() } }
    }

    /**
     * Java-friendly setter for [fileNameProvider].
     * Avoids requiring Java callers to implement a Kotlin `Function0<String>`.
     */
    fun setFileNameProvider(supplier: JavaStringSupplier?) {
        fileNameProvider = supplier?.let { { it.get() } }
    }
}