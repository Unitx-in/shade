package com.unitx.shade_core.common.config.scope

import com.unitx.shade_core.common.config.base.CameraConfig
import com.unitx.shade_core.common.config.base.GalleryConfig
import com.unitx.shade_core.interop.JavaUnitCallback

/**
 * DSL scope for image configuration. Use inside `rememberShade { }` or `Shade.with(this) { }`.
 *
 * ```kotlin
 * image {
 *     camera {
 *         compress { enabled = true; quality = 80 }
 *         onResult { result -> /* result.file, result.uri */ }
 *         onFailure { error -> }
 *     }
 *     gallery {
 *         multiSelect { enabled = true; maxItems = 5 }
 *         copyToCache { enabled = true }
 *         onResult { result -> /* ShadeResult.Single or .Multiple */ }
 *         onFailure { error -> }
 *     }
 * }
 * ```
 *
 * Only configure the blocks you need — unused blocks are simply not registered.
 */
class ImageScope {
    internal var camera: CameraConfig? = null
    internal var gallery: GalleryConfig? = null

    /** Configures image capture via the device camera. */
    fun camera(block: CameraConfig.() -> Unit) {
        camera = CameraConfig().apply(block)
    }

    /**
     * Java-friendly overload of [camera]. Avoids requiring `return null;`
     * from Java lambdas.
     *
     * Configures image capture via the device camera.
     */
    fun camera(block: JavaUnitCallback<CameraConfig>) {
        camera = CameraConfig().apply { block.invoke(this) }
    }

    /** Configures image selection from the system photo picker. */
    fun gallery(block: GalleryConfig.() -> Unit) {
        gallery = GalleryConfig().apply(block)
    }

    /**
     * Java-friendly overload of [gallery]. Avoids requiring `return null;`
     * from Java lambdas.
     *
     * Configures image selection from the system photo picker.
     */
    fun gallery(block: JavaUnitCallback<GalleryConfig>) {
        gallery = GalleryConfig().apply { block.invoke(this) }
    }
}