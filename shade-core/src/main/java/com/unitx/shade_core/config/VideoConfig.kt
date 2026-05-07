package com.unitx.shade_core.config

import android.net.Uri
import com.unitx.shade_core.result.ShadeError

class VideoConfig {
    internal var onResult: ((Uri) -> Unit)? = null
    internal var onFailure: ((ShadeError) -> Unit)? = null

    fun onResult(block: (Uri) -> Unit) {
        onResult = block
    }

    fun onFailure(block: (ShadeError) -> Unit) {
        onFailure = block
    }
}