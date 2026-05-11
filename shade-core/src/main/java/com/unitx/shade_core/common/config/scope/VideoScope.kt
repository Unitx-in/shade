package com.unitx.shade_core.common.config.scope

import com.unitx.shade_core.common.config.base.CameraConfig
import com.unitx.shade_core.common.config.base.GalleryConfig

/**
 * DSL scope for video-related configuration.
 *
 * Mirrors [ImageScope] exactly — camera captures, gallery picks,
 * and the same multi-select support on the gallery side.
 *
 * ```
 * shade = Shade.with(this) {
 *     video {
 *         camera {
 *             onResult { result -> /* ShadeResult.Captured — has .file + .uri */ }
 *             onFailure { error -> }
 *         }
 *         gallery {
 *             onResult { result -> /* ShadeResult.Single */ }
 *             onFailure { error -> }
 *         }
 *     }
 * }
 * ```
 */
class VideoScope {
    internal var camera: CameraConfig? = null
    internal var gallery: GalleryConfig? = null

    fun camera(block: CameraConfig.() -> Unit) {
        camera = CameraConfig().apply(block)
    }

    fun gallery(block: GalleryConfig.() -> Unit) {
        gallery = GalleryConfig().apply(block)
    }
}