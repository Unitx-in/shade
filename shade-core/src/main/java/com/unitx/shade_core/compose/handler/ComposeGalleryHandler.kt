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
import com.unitx.shade_core.common.result.ShadeCompressionException
import com.unitx.shade_core.common.result.ShadeFileSaveException
import com.unitx.shade_core.common.result.ShadeResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

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
                try {
                    val processed = ImageProcessor.process(
                        context = context,
                        uri = single.uri,
                        prefix = "IMG_",
                        extension = ".jpg",
                        copyToCache = gallery.copyToCache,
                        compression = gallery.compress,
                    )

                    gallery.onResult?.invoke(
                        ShadeResult.Single(uri = processed.uri, file = processed.file)
                    )

                } catch (e: ShadeFileSaveException) {
                    gallery.onFailure?.invoke(
                        ShadeError.FileSaveFailed(uri = e.uri, cause = e.cause)
                    )
                } catch (e: ShadeCompressionException) {
                    gallery.onFailure?.invoke(
                        ShadeError.CompressionFailed(
                            source = ShadeError.CompressionSource.Image,
                            cause = e.cause
                        )
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    gallery.onFailure?.invoke(ShadeError.Unknown(e))
                }
            }
        }

        imageGallerySingleCallback.onFailure = {
            config.image?.gallery?.onFailure?.invoke(it)
        }

        // ── Image multi ─────────────────────────────────────────────────────────

        imageGalleryMultiCallback.onResult = onResult@{ result ->

            val multiple = result as ShadeResult.Multiple
            val gallery = config.image?.gallery ?: return@onResult
            val uris = multiple.items.map { it.uri }

            scope.launch {
                try {
                    val items = ImageProcessor.process(
                        context = context,
                        uris = uris,
                        prefix = "IMG_",
                        extension = ".jpg",
                        copyToCache = gallery.copyToCache,
                        compression = gallery.compress
                    )

                    gallery.onResult?.invoke(ShadeResult.Multiple(items))

                } catch (e: ShadeFileSaveException) {
                    gallery.onFailure?.invoke(
                        ShadeError.FileSaveFailed(uris = e.uris, cause = e.cause)
                    )
                } catch (e: ShadeCompressionException) {
                    val failedUris = e.failedIndices.mapNotNull { uris.getOrNull(it) }
                    gallery.onFailure?.invoke(
                        ShadeError.CompressionFailed(
                            source = ShadeError.CompressionSource.Image,
                            cause = e.cause,
                            failedUris = failedUris
                        )
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    gallery.onFailure?.invoke(ShadeError.Unknown(e))
                }
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
                try {
                    val processed = VideoProcessor.process(
                        context = context,
                        uri = single.uri,
                        prefix = "VID_",
                        extension = ".mp4",
                        copyToCache = gallery.copyToCache,
                        compression = gallery.compress
                    )

                    gallery.onResult?.invoke(
                        ShadeResult.Single(uri = processed.uri, file = processed.file)
                    )

                } catch (e: ShadeFileSaveException) {
                    gallery.onFailure?.invoke(
                        ShadeError.FileSaveFailed(uri = e.uri, cause = e.cause)
                    )
                } catch (e: ShadeCompressionException) {
                    gallery.onFailure?.invoke(
                        ShadeError.CompressionFailed(
                            source = ShadeError.CompressionSource.Video,
                            cause = e.cause
                        )
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    gallery.onFailure?.invoke(ShadeError.Unknown(e))
                }
            }
        }

        videoGallerySingleCallback.onFailure = {
            config.video?.gallery?.onFailure?.invoke(it)
        }

        // ── Video multi ─────────────────────────────────────────────────────────
        videoGalleryMultiCallback.onResult = onResult@{ result ->

            val multiple = result as ShadeResult.Multiple
            val gallery = config.video?.gallery ?: return@onResult
            val uris = multiple.items.map { it.uri }

            scope.launch {
                try {
                    val items = VideoProcessor.process(
                        context = context,
                        uris = uris,
                        prefix = "VID_",
                        extension = ".mp4",
                        copyToCache = gallery.copyToCache,
                        compression = gallery.compress
                    )

                    gallery.onResult?.invoke(ShadeResult.Multiple(items))

                } catch (e: ShadeFileSaveException) {
                    gallery.onFailure?.invoke(
                        ShadeError.FileSaveFailed(uris = e.uris, cause = e.cause)
                    )
                } catch (e: ShadeCompressionException) {
                    val failedUris = e.failedIndices.mapNotNull { uris.getOrNull(it) }
                    gallery.onFailure?.invoke(
                        ShadeError.CompressionFailed(
                            source = ShadeError.CompressionSource.Video,
                            cause = e.cause,
                            failedUris = failedUris
                        )
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    gallery.onFailure?.invoke(ShadeError.Unknown(e))
                }
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