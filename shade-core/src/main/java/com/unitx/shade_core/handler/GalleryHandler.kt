package com.unitx.shade_core.handler

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import com.unitx.shade_core.common.FileHelper
import com.unitx.shade_core.common.PermissionHelper
import com.unitx.shade_core.common.config.ShadeConfig
import com.unitx.shade_core.core.LauncherRegistry
import com.unitx.shade_core.common.result.ShadeError
import com.unitx.shade_core.common.result.ShadeResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
    private val registry: LauncherRegistry,
    private val scope: CoroutineScope
) {

    init {
        registry.onMediaPermissionResult = result@{ granted->
            if (!granted){
                val permission = PermissionHelper.readVideoPermission()

                val error = if (PermissionHelper.shouldShowRationale(context, permission)) ShadeError.PermissionDenied
                else ShadeError.PermissionPermanentlyDenied

                config.video?.gallery?.onFailure?.invoke(error)
                return@result
            }
            launchVideoGallery()
        }

        registry.onImageGallerySingle = onImageGallerySingle@{ uri ->
            val gallery = config.image?.gallery ?: return@onImageGallerySingle

            handleSingleGalleryResult(
                uri = uri,
                copyToCache = gallery.copyToCache,
                prefix = "IMG_",
                extension = ".jpg",
                onFailure = gallery.onFailure,
                onResult = gallery.onResult
            )
        }

        registry.onVideoGallerySingle = onVideoGallerySingle@{ uri ->
            val gallery = config.video?.gallery ?: return@onVideoGallerySingle

            handleSingleGalleryResult(
                uri = uri,
                copyToCache = gallery.copyToCache,
                prefix = "VID_",
                extension = ".mp4",
                onFailure = gallery.onFailure,
                onResult = gallery.onResult
            )
        }

        registry.onImageGalleryMulti = onImageGalleryMulti@{ uris ->
            val gallery = config.image?.gallery
                ?: return@onImageGalleryMulti

            handleMultiGalleryResult(
                uris = uris,
                copyToCache = gallery.copyToCache,
                prefix = "IMG_",
                extension = ".jpg",
                onFailure = gallery.onFailure,
                onResult = gallery.onResult
            )
        }

        registry.onVideoGalleryMulti = onVideoGalleryMulti@{ uris ->
            val gallery = config.video?.gallery
                ?: return@onVideoGalleryMulti

            handleMultiGalleryResult(
                uris = uris,
                copyToCache = gallery.copyToCache,
                prefix = "VID_",
                extension = ".mp4",
                onFailure = gallery.onFailure,
                onResult = gallery.onResult
            )
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

    private fun handleSingleGalleryResult(
        uri: Uri?,
        copyToCache: Boolean,
        prefix: String,
        extension: String,
        onFailure: ((ShadeError) -> Unit)?,
        onResult: ((ShadeResult.Single) -> Unit)?
    ) {
        scope.launch {
            if (uri == null) {
                onFailure?.invoke(ShadeError.PickCancelled)
                return@launch
            }

            val file = if (copyToCache) {
                FileHelper.copyUriToCache(
                    context,
                    uri,
                    prefix,
                    extension
                )
            } else null

            if (copyToCache && file == null) {
                onFailure?.invoke(ShadeError.FileSaveFailed)
                return@launch
            }

            onResult?.invoke(
                ShadeResult.Single(uri, file)
            )
        }
    }

    private fun handleMultiGalleryResult(
        uris: List<Uri>,
        copyToCache: Boolean,
        prefix: String,
        extension: String,
        onFailure: ((ShadeError) -> Unit)?,
        onResult: ((ShadeResult.Multiple) -> Unit)?
    ) {
        scope.launch {
            if (uris.isEmpty()) {
                onFailure?.invoke(ShadeError.PickCancelled)
                return@launch
            }

            val items = mutableListOf<ShadeResult.ShadeMedia>()

            for (uri in uris) {

                val file = if (copyToCache) {
                    FileHelper.copyUriToCache(
                        context,
                        uri,
                        prefix,
                        extension
                    )
                } else null

                if (copyToCache && file == null) {
                    onFailure?.invoke(ShadeError.FileSaveFailed)
                    return@launch
                }

                items += ShadeResult.ShadeMedia(uri, file)
            }

            onResult?.invoke(
                ShadeResult.Multiple(items)
            )
        }
    }

    private fun launchVideoGallery() {
        val galleryConfig = config.video?.gallery ?: return
        if (galleryConfig.isMultiSelect)
            registry.videoGalleryMultiLauncher.launch(PickVisualMediaRequest(PickVisualMedia.VideoOnly))
        else
            registry.videoGallerySingleLauncher.launch(PickVisualMediaRequest(PickVisualMedia.VideoOnly))
    }
}