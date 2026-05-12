package com.unitx.shade_core.common.config

import com.unitx.shade_core.common.config.base.DocumentConfig
import com.unitx.shade_core.common.config.scope.ImageScope
import com.unitx.shade_core.common.config.scope.VideoScope

/**
 * Root DSL configuration for Shade. Passed to `rememberShade { }` or `Shade.with(this) { }`.
 *
 * Configure only the media types you need — unused blocks are not registered,
 * so no unnecessary permissions or launchers are created.
 *
 * ```kotlin
 * val shade = rememberShade {
 *     image {
 *         camera { onResult { } }
 *         gallery { onResult { } }
 *     }
 *     video {
 *         gallery {
 *             multiSelect { enabled = true }
 *             onResult { }
 *         }
 *     }
 *     document {
 *         copyToCache { enabled = true }
 *         onResult { }
 *     }
 * }
 *
 * shade.launch(ShadeAction.Image.Camera)
 * ```
 *
 * @see ImageScope
 * @see VideoScope
 * @see DocumentConfig
 * @see ShadeAction
 */
class ShadeConfig {

    internal var image: ImageScope? = null
    internal var video: VideoScope? = null
    internal var document: DocumentConfig? = null

    /** Configures image camera capture and/or gallery picking. */
    fun image(block: ImageScope.() -> Unit) {
        image = ImageScope().apply(block)
    }

    /** Configures video camera capture and/or gallery picking. */
    fun video(block: VideoScope.() -> Unit) {
        video = VideoScope().apply(block)
    }

    /** Configures document picking. MIME types are specified at launch via [ShadeAction.Document]. */
    fun document(block: DocumentConfig.() -> Unit) {
        document = DocumentConfig().apply(block)
    }
}
