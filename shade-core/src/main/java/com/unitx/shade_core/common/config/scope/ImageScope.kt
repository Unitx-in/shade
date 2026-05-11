package com.unitx.shade_core.common.config.scope

import com.unitx.shade_core.common.config.base.CameraConfig
import com.unitx.shade_core.common.config.base.GalleryConfig

/**
 * DSL scope for image-related configuration.
 *
 * ```
 * shade = Shade.with(this) {
 *     image {
 *         camera {
 *             onResult { result -> /* ShadeResult.Captured */ }
 *             onFailure { error -> }
 *         }
 *         gallery {
 *             multiSelect(maxItems = 3)
 *             onResult { result -> /* ShadeResult.Single or .Multiple */ }
 *             onFailure { error -> }
 *         }
 *     }
 * }
 * ```
 */
class ImageScope {
    internal var camera: CameraConfig? = null
    internal var gallery: GalleryConfig? = null

    fun camera(block: CameraConfig.() -> Unit) {
        camera = CameraConfig().apply(block)
    }

    fun gallery(block: GalleryConfig.() -> Unit) {
        gallery = GalleryConfig().apply(block)
    }
}