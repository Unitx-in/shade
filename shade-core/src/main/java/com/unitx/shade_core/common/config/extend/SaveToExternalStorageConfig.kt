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
 *         Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
 *         "MyApp"
 *     )
 * }
 * ```
 *
 * @see CameraConfig
 */
class SaveToExternalStorageConfig {
    /** Whether external storage saving is enabled. */
    var enabled: Boolean = false

    /** Target directory where the file will be saved. Created automatically if it does not exist. */
    var path: File? = null
}