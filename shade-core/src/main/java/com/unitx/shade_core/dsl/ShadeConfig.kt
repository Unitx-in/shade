package com.unitx.shade_core.dsl

/**
 * Root DSL configuration block for Shade.
 *
 * Pass this to [Shade.with] to describe every media interaction your
 * screen needs. You only need to configure the types you actually use.
 *
 * ```kotlin
 * shade = Shade.with(fragment = this) {
 *
 *     image {
 *         camera {
 *             onResult { result -> }
 *             onFailure { error -> }
 *         }
 *         gallery {
 *             multiSelect(maxItems = 4)
 *             onResult { result -> }
 *             onFailure { error -> }
 *         }
 *     }
 *
 *     video {
 *         camera {
 *             onResult { result -> }
 *             onFailure { error -> }
 *         }
 *         gallery {
 *             onResult { result -> }
 *             onFailure { error -> }
 *         }
 *     }
 *
 *     pdf {
 *         onResult { result -> }
 *         onFailure { error -> }
 *     }
 *
 *     document {
 *         copyToCache = true
 *         onResult { result -> }
 *         onFailure { error -> }
 *     }
 * }
 * ```
 */
class ShadeConfig {

    internal var image: ImageScope? = null
    internal var video: VideoScope? = null
    internal var pdf: PdfConfig? = null
    internal var document: DocumentConfig? = null

    fun image(block: ImageScope.() -> Unit) {
        image = ImageScope().apply(block)
    }

    fun video(block: VideoScope.() -> Unit) {
        video = VideoScope().apply(block)
    }

    fun pdf(block: PdfConfig.() -> Unit) {
        pdf = PdfConfig().apply(block)
    }

    fun document(block: DocumentConfig.() -> Unit) {
        document = DocumentConfig().apply(block)
    }
}