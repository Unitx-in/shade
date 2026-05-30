package com.unitx.shade_core.common.result

import android.net.Uri

internal class ShadeFileSaveException(
    val uri: Uri? = null,
    val uris: List<Uri> = emptyList(),
    cause: Throwable? = null
) : Exception("Failed to save file(s) to cache", cause)