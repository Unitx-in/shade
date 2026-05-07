package com.unitx.shade_core.common

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import com.unitx.shade_core.config.ShadeConfig

object PickerHelper {
    internal fun launchVideoGalleryInternal(
        config: ShadeConfig,
        singleLauncher: ActivityResultLauncher<PickVisualMediaRequest>?,
        multiLauncher: ActivityResultLauncher<PickVisualMediaRequest>?,
    ) {
        val g = config.video?.gallery ?: return
        if (g.isMultiSelect) multiLauncher?.launch(PickVisualMediaRequest(PickVisualMedia.VideoOnly))
        else singleLauncher?.launch(PickVisualMediaRequest(PickVisualMedia.VideoOnly))
    }
}