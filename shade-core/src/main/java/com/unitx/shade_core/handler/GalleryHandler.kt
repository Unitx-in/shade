package com.unitx.shade_core.handler

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import com.unitx.shade_core.common.PermissionHelper
import com.unitx.shade_core.common.config.ShadeConfig
import com.unitx.shade_core.core.LauncherRegistry
import com.unitx.shade_core.common.result.ShadeError
import com.unitx.shade_core.common.result.ShadeResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.unitx.shade_core.common.compressor.ImageProcessor
import com.unitx.shade_core.common.compressor.VideoProcessor
import com.unitx.shade_core.common.config.extend.CompressionConfig
import com.unitx.shade_core.common.config.extend.CacheConfig
import com.unitx.shade_core.common.config.base.GalleryConfig

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

        registry.onMediaPermissionResult = result@{ granted ->

            if (!granted) {

                val permission = PermissionHelper.readVideoPermission()

                val error =
                    if (PermissionHelper.shouldShowRationale(context, permission))
                        ShadeError.PermissionDenied
                    else
                        ShadeError.PermissionPermanentlyDenied

                config.video?.gallery?.onFailure?.invoke(error)
                return@result
            }

            launchVideoGallery()
        }

        registry.onImageGallerySingle = onImageGallerySingle@{ uri ->

            val gallery = config.image?.gallery
                ?: return@onImageGallerySingle

            handleSingleGalleryResult(
                uri = uri,
                prefix = "IMG_",
                extension = ".jpg",
                copyToCache = gallery.copyToCache,
                processor = { pickedUri, pref, ext, copyToCache, compression ->

                    ImageProcessor.process(
                        context = context,
                        uri = pickedUri,
                        prefix = pref,
                        extension = ext,
                        copyToCache = copyToCache,
                        compression = compression,
                    )
                },
                compression = gallery.compress,
                onFailure = gallery.onFailure,
                onResult = gallery.onResult
            )
        }

        registry.onVideoGallerySingle = onVideoGallerySingle@{ uri ->

            val gallery = config.video?.gallery
                ?: return@onVideoGallerySingle

            handleSingleGalleryResult(
                uri = uri,
                prefix = "VID_",
                extension = ".mp4",
                copyToCache = gallery.copyToCache,
                processor = { pickedUri, pref, ext, copyToCache, compression ->

                    VideoProcessor.process(
                        context = context,
                        uri = pickedUri,
                        prefix = pref,
                        extension = ext,
                        copyToCache = copyToCache,
                        compression = compression
                    )
                },
                compression = gallery.compress,
                onFailure = gallery.onFailure,
                onResult = gallery.onResult
            )
        }

        registry.onImageGalleryMulti = onImageGalleryMulti@{ uris ->

            val gallery = config.image?.gallery
                ?: return@onImageGalleryMulti

            handleMultiGalleryResult(
                uris = uris,
                prefix = "IMG_",
                extension = ".jpg",
                copyToCache = gallery.copyToCache,
                processor = { uris, pref, ext, copyToCache, compression ->

                    ImageProcessor.process(
                        context = context,
                        uris = uris,
                        prefix = pref,
                        extension = ext,
                        copyToCache = copyToCache,
                        compression = compression,
                    )
                },
                compression = gallery.compress,
                onFailure = gallery.onFailure,
                onResult = gallery.onResult
            )
        }

        registry.onVideoGalleryMulti = onVideoGalleryMulti@{ uris ->

            val gallery = config.video?.gallery
                ?: return@onVideoGalleryMulti

            handleMultiGalleryResult(
                uris = uris,
                prefix = "VID_",
                extension = ".mp4",
                copyToCache = gallery.copyToCache,
                processor = { uris, pref, ext, copyToCache, compression ->

                    VideoProcessor.process(
                        context = context,
                        uris = uris,
                        prefix = pref,
                        extension = ext,
                        copyToCache = copyToCache,
                        compression = compression
                    )
                },
                compression = gallery.compress,
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
        prefix: String,
        extension: String,
        copyToCache: CacheConfig?,
        processor: suspend (
            uri: Uri,
            prefix: String,
            extension: String,
            copyToCache: CacheConfig?,
            compression: CompressionConfig?
        ) -> ShadeResult.ShadeMedia,
        compression: CompressionConfig?,
        onFailure: ((ShadeError) -> Unit)?,
        onResult: ((ShadeResult.Single) -> Unit)?
    ) {
        scope.launch {

            if (uri == null) {
                onFailure?.invoke(ShadeError.PickCancelled)
                return@launch
            }

            val media = processor(
                uri,
                prefix,
                extension,
                copyToCache,
                compression
            )

            if (compression?.enabled == true && media.file == null) {

                onFailure?.invoke(
                    ShadeError.CompressionFailed
                )

                return@launch
            }

            onResult?.invoke(
                ShadeResult.Single(
                    uri = media.uri,
                    file = media.file
                )
            )
        }
    }

    private fun handleMultiGalleryResult(
        uris: List<Uri>,
        prefix: String,
        extension: String,
        copyToCache: CacheConfig?,
        processor: suspend (
            uris: List<Uri>,
            prefix: String,
            extension: String,
            copyToCache: CacheConfig?,
            compression: CompressionConfig?
        ) -> List<ShadeResult.ShadeMedia>,
        compression: CompressionConfig?,
        onFailure: ((ShadeError) -> Unit)?,
        onResult: ((ShadeResult.Multiple) -> Unit)?
    ) {
        scope.launch {

            if (uris.isEmpty()) {
                onFailure?.invoke(ShadeError.PickCancelled)
                return@launch
            }

            val items = processor(
                uris,
                prefix,
                extension,
                copyToCache,
                compression
            )

            if (compression?.enabled == true && items.any { it.file == null }) {
                onFailure?.invoke(ShadeError.CompressionFailed)
                return@launch
            }

            onResult?.invoke(ShadeResult.Multiple(items))
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