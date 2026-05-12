package com.unitx.shade_core.common.compressor

/**
 * Output format for compressed images.
 *
 * Used in the `compress { }` configuration block to control
 * the encoding format of the resulting image file.
 *
 * ## Usage
 * ```kotlin
 * compress {
 *     enabled = true
 *     format = CompressFormat.PNG
 * }
 * ```
 */
enum class CompressFormat {

    /**
     * JPEG format. Lossy compression — smaller file size, no transparency support.
     *
     * The output quality is controlled by the `quality` parameter (0–100).
     * This is the default format and recommended for photos.
     */
    JPEG,

    /**
     * PNG format. Lossless compression — larger file size, supports transparency.
     *
     * The `quality` parameter is ignored for PNG — output is always lossless.
     * Recommended for screenshots, graphics, or images requiring transparency.
     */
    PNG
}