package com.unitx.shade_core.compose.handler

import android.content.Context
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import com.unitx.shade_core.common.PermissionHelper
import com.unitx.shade_core.compose.state.PermissionCallbackHolder
import com.unitx.shade_core.compose.state.ShadeResultHolder
import com.unitx.shade_core.common.config.ShadeConfig
import com.unitx.shade_core.common.result.ShadeError

/**
 * Compose-side gallery handler.
 *
 * Wires gallery result callbacks and media permission, and exposes
 * [handleImageGallery] / [handleVideoGallery] to [ComposeShadeCore].
 */
internal class ComposeGalleryHandler(
    private val context: Context,
    private val config: ShadeConfig,
    private val permCallbacks: PermissionCallbackHolder,
    private val mediaPermLauncher: ActivityResultLauncher<String>?,
    private val imageGallerySingleLauncher: ActivityResultLauncher<PickVisualMediaRequest>?,
    private val imageGalleryMultiLauncher: ActivityResultLauncher<PickVisualMediaRequest>?,
    private val videoGallerySingleLauncher: ActivityResultLauncher<PickVisualMediaRequest>?,
    private val videoGalleryMultiLauncher: ActivityResultLauncher<PickVisualMediaRequest>?,
    imageGallerySingleCallback: ShadeResultHolder,
    imageGalleryMultiCallback: ShadeResultHolder,
    videoGallerySingleCallback: ShadeResultHolder,
    videoGalleryMultiCallback: ShadeResultHolder,
) {

    init {
        // ── Image gallery results ─────────────────────────────────────────────
        imageGallerySingleCallback.onResult  = { config.image?.gallery?.onResult?.invoke(it) }
        imageGallerySingleCallback.onFailure = { config.image?.gallery?.onFailure?.invoke(it) }

        imageGalleryMultiCallback.onResult  = { config.image?.gallery?.onResult?.invoke(it) }
        imageGalleryMultiCallback.onFailure = { config.image?.gallery?.onFailure?.invoke(it) }

        // ── Video gallery results ─────────────────────────────────────────────
        videoGallerySingleCallback.onResult  = { config.video?.gallery?.onResult?.invoke(it) }
        videoGallerySingleCallback.onFailure = { config.video?.gallery?.onFailure?.invoke(it) }

        videoGalleryMultiCallback.onResult  = { config.video?.gallery?.onResult?.invoke(it) }
        videoGalleryMultiCallback.onFailure = { config.video?.gallery?.onFailure?.invoke(it) }

        // ── Media permission result ───────────────────────────────────────────
        permCallbacks.onMedia = { granted ->
            if (granted) {
                launchVideoGallery()
            } else {
                val perm  = PermissionHelper.readVideoPermission()
                val error = if (PermissionHelper.shouldShowRationale(context, perm))
                    ShadeError.PermissionDenied
                else
                    ShadeError.PermissionPermanentlyDenied
                config.video?.gallery?.onFailure?.invoke(error)
            }
        }
    }

    fun handleImageGallery() {
        val g = config.image?.gallery ?: return
        if (g.isMultiSelect)
            imageGalleryMultiLauncher?.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        else
            imageGallerySingleLauncher?.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
    }

    fun handleVideoGallery() {
        config.video?.gallery ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perm = PermissionHelper.readVideoPermission()
            if (!PermissionHelper.hasPermission(context, perm)) {
                mediaPermLauncher?.launch(perm)
                return
            }
        }
        launchVideoGallery()
    }

    private fun launchVideoGallery() {
        val g = config.video?.gallery ?: return
        if (g.isMultiSelect)
            videoGalleryMultiLauncher?.launch(PickVisualMediaRequest(PickVisualMedia.VideoOnly))
        else
            videoGallerySingleLauncher?.launch(PickVisualMediaRequest(PickVisualMedia.VideoOnly))
    }
}