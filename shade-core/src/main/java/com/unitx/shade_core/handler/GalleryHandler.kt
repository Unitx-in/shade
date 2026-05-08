package com.unitx.shade_core.handler

import android.content.Context
import android.os.Build
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import com.unitx.shade_core.common.FileHelper
import com.unitx.shade_core.common.PermissionHelper
import com.unitx.shade_core.common.config.ShadeConfig
import com.unitx.shade_core.core.LauncherRegistry
import com.unitx.shade_core.common.result.ShadeError
import com.unitx.shade_core.common.result.ShadeResult

/**
 * Handles all gallery-related media flows — image and video picking,
 * single and multi-select, and the API 33+ media permission for video.
 *
 * Responsibilities:
 * - Media permission request and result routing (video gallery, API 33+)
 * - Routing to single vs multi-select launcher based on [GalleryConfig.isMultiSelect]
 * - Dispatching [ShadeResult.Single] / [ShadeResult.Multiple] or [ShadeError]
 *   to the DSL config callbacks
 */
internal class GalleryHandler(
    private val context: Context,
    private val config: ShadeConfig,
    private val registry: LauncherRegistry
) {

    init {
        registry.onMediaPermissionResult = { granted ->
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

        registry.onImageGallerySingle = { uri ->
            if (uri != null) {
                val file = if (config.image?.gallery?.copyToCache == true)
                    FileHelper.copyUriToCache(context, uri, "IMG_", ".jpg")
                else null
                config.image?.gallery?.onResult?.invoke(ShadeResult.Single(uri, file))
            } else {
                config.image?.gallery?.onFailure?.invoke(ShadeError.PickCancelled)
            }
        }

        registry.onImageGalleryMulti = { uris ->
            if (uris.isNotEmpty()) {
                val items = uris.map { uri ->
                    val file = if (config.image?.gallery?.copyToCache == true)
                        FileHelper.copyUriToCache(context, uri, "IMG_", ".jpg")
                    else null
                    ShadeResult.ShadeMedia(uri, file)
                }
                config.image?.gallery?.onResult?.invoke(ShadeResult.Multiple(items))
            } else {
                config.image?.gallery?.onFailure?.invoke(ShadeError.PickCancelled)
            }
        }

        registry.onVideoGallerySingle = { uri ->
            if (uri != null) {
                val file = if (config.video?.gallery?.copyToCache == true)
                    FileHelper.copyUriToCache(context, uri, "VID_", ".mp4")
                else null
                config.video?.gallery?.onResult?.invoke(ShadeResult.Single(uri, file))
            } else {
                config.video?.gallery?.onFailure?.invoke(ShadeError.PickCancelled)
            }
        }

        registry.onVideoGalleryMulti = { uris ->
            if (uris.isNotEmpty()) {
                val items = uris.map { uri ->
                    val file = if (config.video?.gallery?.copyToCache == true)
                        FileHelper.copyUriToCache(context, uri, "VID_", ".mp4")
                    else null
                    ShadeResult.ShadeMedia(uri, file)
                }
                config.video?.gallery?.onResult?.invoke(ShadeResult.Multiple(items))
            } else {
                config.video?.gallery?.onFailure?.invoke(ShadeError.PickCancelled)
            }
        }
    }

    fun handleImageGallery() {
        val galleryConfig = config.image?.gallery ?: return
        if (galleryConfig.isMultiSelect)
            registry.imageGalleryMultiLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        else
            registry.imageGallerySingleLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
    }

    fun handleVideoGallery() {
        config.video?.gallery ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perm = PermissionHelper.readVideoPermission()
            if (!PermissionHelper.hasPermission(context, perm)) {
                registry.mediaPermissionLauncher.launch(perm)
                return
            }
        }
        launchVideoGallery()
    }

    private fun launchVideoGallery() {
        val galleryConfig = config.video?.gallery ?: return
        if (galleryConfig.isMultiSelect)
            registry.videoGalleryMultiLauncher.launch(PickVisualMediaRequest(PickVisualMedia.VideoOnly))
        else
            registry.videoGallerySingleLauncher.launch(PickVisualMediaRequest(PickVisualMedia.VideoOnly))
    }
}