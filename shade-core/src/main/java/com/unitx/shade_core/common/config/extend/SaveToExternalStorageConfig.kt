package com.unitx.shade_core.common.config.extend

import java.io.File

/**
 * Configuration for saving captured files to external storage.
 *
 * When enabled, the final file (compressed or raw) is written to [path]
 * and the cache copy is deleted automatically.
 *
 * ```kotlin
 * saveToExternalStorage {
 *     enabled = true
 *     path = File(
 *         context.getExternalFilesDir(null),
 *         "MyApp"
 *     )
 *     fileName = "custom_name.jpg"
 * }
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
     * Optional custom file name (including extension) to use when saving.
     * If null, the original captured file's name is kept.
     */
    var fileName: String? = null
}