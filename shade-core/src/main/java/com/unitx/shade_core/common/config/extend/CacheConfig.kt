package com.unitx.shade_core.common.config.extend

import com.unitx.shade_core.interop.JavaUnitCallback

/**
 * Configuration for copying picked files to the app's cache directory.
 *
 * When enabled, each picked file is copied to cache and delivered as a
 * stable [java.io.File] in the result. Without this, only the URI is available.
 *
 * ```kotlin
 * copyToCache {
 *     enabled = true
 *     onProgress = { config ->
 *         config as ProgressConfig.Copying
 *         Log.d("Tag", "File ${config.fileNumber}: ${config.percent}%")
 *     }
 * }
 * ```
 */
class CacheConfig {

    /** Whether cache copying is active. Default: `true`. */
    var enabled: Boolean = true

    /**
     * Progress callback. Cast to [ProgressConfig.Copying] for
     * per-file percent and file number.
     */
    var onProgress: ((ProgressConfig) -> Unit)? = null

    /**
     * Java-friendly setter for [onProgress]. Avoids requiring `return null;`
     * from Java lambdas, since `var onProgress = ...` isn't directly settable
     * with a clean lambda from Java.
     *
     * Progress callback. Cast to [ProgressConfig.Copying] for
     * per-file percent and file number.
     */
    fun onProgress(block: JavaUnitCallback<ProgressConfig>) {
        onProgress = { block.invoke(it) }
    }
}