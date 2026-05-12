package com.unitx.shade_core.common.config.scope

import com.unitx.shade_core.common.config.base.CameraConfig
import com.unitx.shade_core.common.config.base.GalleryConfig

/**
 * DSL scope for video configuration. Use inside `rememberShade { }` or `Shade.with(this) { }`.
 *
 * ```kotlin
 * video {
 *     camera {
 *         compress { enabled = true; videoBitrate = 1_500_000 }
 *         onResult { result -> /* result.file, result.uri */ }
 *         onFailure { error -> }
 *     }
 *     gallery {
 *         multiSelect { enabled = true; maxItems = 3 }
 *         copyToCache { enabled = true }
 *         onResult { result -> /* ShadeResult.Single or .Multiple */ }
 *         onFailure { error -> }
 *     }
 * }
 * ```
 *
 * Only configure the blocks you need — unused blocks are simply not registered.
 */
class VideoScope {
    internal var camera: CameraConfig? = null
    internal var gallery: GalleryConfig? = null

    /** Configures video capture via the device camera. */
    fun camera(block: CameraConfig.() -> Unit) {
        camera = CameraConfig().apply(block)
    }

    /** Configures video selection from the system video picker. */
    fun gallery(block: GalleryConfig.() -> Unit) {
        gallery = GalleryConfig().apply(block)
    }
}