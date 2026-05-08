package com.unitx.shade_core.compose.state

import android.net.Uri
import java.io.File

/**
 * Holds mutable references to the temp file and URI written by the camera
 * launch function and read by the camera result callback. Exists because
 * the two closures are created at different points in [rememberShade].
 */
internal class CaptureState {
    var file: File? = null
    var uri: Uri?   = null
    fun clear() { file = null; uri = null }
}