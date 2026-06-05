package com.unitx.shade_core.common.videoLimit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import com.unitx.shade_core.common.config.extend.VideoCameraConfig

internal class CaptureVideoWithLimit(private val videoCameraConfig: VideoCameraConfig?) :
    ActivityResultContract<Uri, Boolean>() {

    override fun createIntent(context: Context, input: Uri): Intent {
        return Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, input)
            videoCameraConfig?.durationLimit?.let { putExtra(MediaStore.EXTRA_DURATION_LIMIT, it) }
            videoCameraConfig?.videoQuality?.let { putExtra(MediaStore.EXTRA_VIDEO_QUALITY, it) }
            videoCameraConfig?.sizeLimit?.let { putExtra(MediaStore.EXTRA_SIZE_LIMIT, it) }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }
}