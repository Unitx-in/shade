package com.unitx.shade_core.compose

import android.net.Uri
import java.io.File

internal class CaptureState {
    var file: File? = null
    var uri: Uri? = null
    fun clear() {
        file = null; uri = null
    }
}