package com.unitx.shade_core.common.config.extend

import com.unitx.shade_core.common.compressor.CompressFormat

/**
 * Configuration for compressing images or videos after capture or picking.
 *
 * Image-specific fields: [quality], [maxWidth], [maxHeight], [format].
 * Video-specific fields: [videoBitrate], [frameRate], [keyFrameInterval].
 * Unused fields for the media type are safely ignored.
 *
 * ```kotlin
 * compress {
 *     enabled = true
 *     quality = 80
 *     maxWidth = 1024
 *     maxHeight = 1024
 *     format = CompressFormat.JPEG
 *     onProgress = { config ->
 *         config as ProgressConfig.Compressing
 *         Log.d("Tag", "File ${config.fileNumber}: ${config.percent}%")
 *     }
 * }
 * ```
 */
class CompressionConfig {

    /** Whether compression is active. Default: `true`. */
    var enabled: Boolean = true

    /** JPEG quality (0–100). Ignored for PNG. Default: `80`. */
    var quality: Int = 80

    /** Max output width in pixels, aspect ratio preserved. Default: `1280`. */
    var maxWidth: Int? = 1280

    /** Max output height in pixels, aspect ratio preserved. Default: `1280`. */
    var maxHeight: Int? = 1280

    /** Output image format. Default: [CompressFormat.JPEG]. */
    var format: CompressFormat = CompressFormat.JPEG

    /** Target video bitrate in bits/sec. Default: `2_000_000` (2 Mbps). */
    var videoBitrate: Int = 2_000_000

    /** Target video frame rate. Default: `30`. */
    var frameRate: Int = 30

    /** Keyframe interval in seconds. Default: `2`. */
    var keyFrameInterval: Int = 2

    /**
     * Progress callback. Cast to [ProgressConfig.Compressing] for
     * per-file percent and file number.
     */
    var onProgress: ((ProgressConfig) -> Unit)? = null
}