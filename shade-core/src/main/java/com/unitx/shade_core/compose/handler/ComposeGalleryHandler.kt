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
import com.unitx.shade_core.common.processor.ImageProcessor
import com.unitx.shade_core.common.processor.VideoProcessor
import com.unitx.shade_core.common.result.ShadeResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

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
    private val scope: CoroutineScope,
) {

    init {

        // ── Image single ────────────────────────────────────────────────────────

        imageGallerySingleCallback.onResult = onResult@{ result ->

            val single = result as ShadeResult.Single
            val gallery = config.image?.gallery ?: return@onResult

            scope.launch {

                val processed = ImageProcessor.process(
                    context = context,
                    uri = single.uri,
                    prefix = "IMG_",
                    extension = ".jpg",
                    copyToCache = gallery.copyToCache,
                    compression = gallery.compress,
                )

                gallery.onResult?.invoke(
                    ShadeResult.Single(
                        uri = processed.uri,
                        file = processed.file
                    )
                )
            }
        }

        imageGallerySingleCallback.onFailure = {
            config.image?.gallery?.onFailure?.invoke(it)
        }

        // ── Image multi ─────────────────────────────────────────────────────────

        imageGalleryMultiCallback.onResult = onResult@{ result ->

            val multiple = result as ShadeResult.Multiple
            val gallery = config.image?.gallery ?: return@onResult

            scope.launch {

//                val existingFiles = multiple.items.map { it.file }

                val items = ImageProcessor.process(
                    context = context,
                    uris = multiple.items.map { it.uri },
//                    files = existingFiles.takeIf { list -> list.any { it != null } },
                    prefix = "IMG_",
                    extension = ".jpg",
                    copyToCache = gallery.copyToCache,
                    compression = gallery.compress
                )

                gallery.onResult?.invoke(
                    ShadeResult.Multiple(items)
                )
            }
        }

        imageGalleryMultiCallback.onFailure = {
            config.image?.gallery?.onFailure?.invoke(it)
        }

        // ── Video single ────────────────────────────────────────────────────────

        videoGallerySingleCallback.onResult = onResult@{ result ->

            val single = result as ShadeResult.Single
            val gallery = config.video?.gallery ?: return@onResult

            scope.launch {

                val processed = VideoProcessor.process(
                    context = context,
                    uri = single.uri,
                    prefix = "VID_",
                    extension = ".mp4",
                    copyToCache = gallery.copyToCache,
                    compression = gallery.compress
                )

                gallery.onResult?.invoke(
                    ShadeResult.Single(
                        uri = processed.uri,
                        file = processed.file
                    )
                )
            }
        }

        videoGallerySingleCallback.onFailure = {
            config.video?.gallery?.onFailure?.invoke(it)
        }

        // ── Video multi ─────────────────────────────────────────────────────────

        videoGalleryMultiCallback.onResult = onResult@{ result ->

            val multiple = result as ShadeResult.Multiple
            val gallery = config.video?.gallery ?: return@onResult

            scope.launch {

//                val existingFiles = multiple.items.map { it.file }

                val items = VideoProcessor.process(
                    context = context,
                    uris = multiple.items.map{ it.uri },
//                    files = existingFiles.takeIf { list-> list.any{ it != null } },
                    prefix = "VID_",
                    extension = ".mp4",
                    copyToCache = gallery.copyToCache,
                    compression = gallery.compress
                )
                
                gallery.onResult?.invoke(
                    ShadeResult.Multiple(items)
                )
            }
        }

        videoGalleryMultiCallback.onFailure = {
            config.video?.gallery?.onFailure?.invoke(it)
        }

        // ── Media permission result ────────────────────────────────────────────

        permCallbacks.onMedia = { granted ->

            if (granted) {
                launchVideoGallery()
            } else {

                val perm = PermissionHelper.readVideoPermission()

                val error =
                    if (PermissionHelper.shouldShowRationale(context, perm))
                        ShadeError.PermissionDenied
                    else
                        ShadeError.PermissionPermanentlyDenied

                config.video?.gallery?.onFailure?.invoke(error)
            }
        }
    }
    fun handleImageGallery() {
        val galleryConfig = config.image?.gallery ?: return
        if (galleryConfig.multiSelect?.enabled == true)
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
        val galleryConfig = config.video?.gallery ?: return
        if (galleryConfig.multiSelect?.enabled == true)
            videoGalleryMultiLauncher?.launch(PickVisualMediaRequest(PickVisualMedia.VideoOnly))
        else
            videoGallerySingleLauncher?.launch(PickVisualMediaRequest(PickVisualMedia.VideoOnly))
    }
}