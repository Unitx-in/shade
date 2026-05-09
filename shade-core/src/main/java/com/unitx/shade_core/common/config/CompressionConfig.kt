package com.unitx.shade_core.common.config

import com.unitx.shade_core.common.compressor.CompressFormat

class CompressionConfig {

    var enabled: Boolean = true

    // image
    var quality: Int = 80
    var maxWidth: Int? = 1280
    var maxHeight: Int? = 1280
    var format: CompressFormat = CompressFormat.JPEG

    // video
    var videoBitrate: Int = 2_000_000
    var frameRate: Int = 30
    var keyFrameInterval: Int = 2
}