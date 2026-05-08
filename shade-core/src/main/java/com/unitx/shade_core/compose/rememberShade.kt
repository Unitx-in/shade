package com.unitx.shade_core.compose

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.unitx.shade_core.common.FileHelper
import com.unitx.shade_core.compose.core.ComposeShadeCore
import com.unitx.shade_core.compose.handler.ComposeCameraHandler
import com.unitx.shade_core.compose.handler.ComposeDocumentHandler
import com.unitx.shade_core.compose.handler.ComposeGalleryHandler
import com.unitx.shade_core.compose.state.CaptureState
import com.unitx.shade_core.compose.state.PermissionCallbackHolder
import com.unitx.shade_core.compose.state.ShadeResultHolder
import com.unitx.shade_core.common.config.ShadeConfig
import com.unitx.shade_core.core.ShadeCore
import com.unitx.shade_core.common.result.ShadeError
import com.unitx.shade_core.common.result.ShadeResult

/**
 * Composable hook that creates and remembers a [ShadeCore] instance.
 *
 * **Only launchers for configured media types are registered.**
 * Each [rememberLauncherForActivityResult] call is gated on whether the
 * corresponding config block is present. Since [ShadeConfig] is built
 * once inside `remember { }` and never mutated, these conditions are
 * stable for the entire lifetime of the composition and will never flip
 * between recompositions.
 *
 * All result wiring and permission logic lives in the three handlers —
 * [ComposeCameraHandler], [ComposeGalleryHandler], [ComposeDocumentHandler].
 * [rememberShade] only registers launchers and assembles them.
 *
 * ```kotlin
 * val shade = rememberShade {
 *     image {
 *         camera {
 *             onResult { result -> viewModel.onImageCaptured(result.file, result.uri) }
 *             onFailure { error -> viewModel.onError(error) }
 *         }
 *         gallery {
 *             multiSelect(maxItems = 5)
 *             onResult { result ->
 *                 when (result) {
 *                     is ShadeResult.Single   -> viewModel.addImage(result.uri)
 *                     is ShadeResult.Multiple -> viewModel.addImages(result.uris)
 *                     else -> Unit
 *                 }
 *             }
 *             onFailure { error -> viewModel.onError(error) }
 *         }
 *     }
 *     pdf {
 *         onResult { result -> viewModel.onPdfPicked(result.uri, result.file!!) }
 *         onFailure { error -> viewModel.onError(error) }
 *     }
 * }
 * ```
 */
@Composable
fun rememberShade(block: ShadeConfig.() -> Unit): ShadeCore {

    val context = LocalContext.current
    val config = remember { ShadeConfig().apply(block) }
    val captureState = remember { CaptureState() }
    val permCallbacks = remember { PermissionCallbackHolder() }

// ── Permission launchers ──────────────────────────────────────────────────

    val cameraPermLauncher =
        rememberPermissionLauncher(
            enabled = config.image?.camera != null || config.video?.camera != null,
            onResult = permCallbacks.onCamera
        )

    val mediaPermLauncher =
        rememberPermissionLauncher(
            enabled = config.video?.gallery != null,
            onResult = permCallbacks.onMedia
        )

// ── Image camera ──────────────────────────────────────────────────────────

    val imageCameraCallback = remember { ShadeResultHolder() }

    val imageCameraLauncher =
        rememberCaptureLauncher(
            enabled = config.image?.camera != null,
            contract = ActivityResultContracts.TakePicture(),
            captureState = captureState,
            callback = imageCameraCallback
        )

// ── Image gallery single ──────────────────────────────────────────────────

    val imageGallerySingleCallback = remember { ShadeResultHolder() }

    val imageGallerySingleLauncher =
        rememberSingleMediaLauncher(
            enabled = config.image?.gallery?.isMultiSelect == false,
            copyToCache = config.image?.gallery?.copyToCache == true,
            prefix = "IMG_",
            extension = ".jpg",
            context = context,
            callback = imageGallerySingleCallback
        )

// ── Image gallery multi ───────────────────────────────────────────────────

    val imageGalleryMultiCallback = remember { ShadeResultHolder() }

    val imageGalleryMultiLauncher =
        rememberMultiMediaLauncher(
            enabled = config.image?.gallery?.isMultiSelect == true,
            maxItems = config.image?.gallery?.maxItems ?: 2,
            copyToCache = config.image?.gallery?.copyToCache == true,
            prefix = "IMG_",
            extension = ".jpg",
            context = context,
            callback = imageGalleryMultiCallback
        )

// ── Video camera ──────────────────────────────────────────────────────────

    val videoCameraCallback = remember { ShadeResultHolder() }

    val videoCameraLauncher =
        rememberCaptureLauncher(
            enabled = config.video?.camera != null,
            contract = ActivityResultContracts.CaptureVideo(),
            captureState = captureState,
            callback = videoCameraCallback
        )

// ── Video gallery single ──────────────────────────────────────────────────

    val videoGallerySingleCallback = remember { ShadeResultHolder() }

    val videoGallerySingleLauncher =
        rememberSingleMediaLauncher(
            enabled = config.video?.gallery?.isMultiSelect == false,
            copyToCache = config.video?.gallery?.copyToCache == true,
            prefix = "VID_",
            extension = ".mp4",
            context = context,
            callback = videoGallerySingleCallback
        )

// ── Video gallery multi ───────────────────────────────────────────────────

    val videoGalleryMultiCallback = remember { ShadeResultHolder() }

    val videoGalleryMultiLauncher =
        rememberMultiMediaLauncher(
            enabled = config.video?.gallery?.isMultiSelect == true,
            maxItems = config.video?.gallery?.maxItems ?: 2,
            copyToCache = config.video?.gallery?.copyToCache == true,
            prefix = "VID_",
            extension = ".mp4",
            context = context,
            callback = videoGalleryMultiCallback
        )

// ── PDF picker ────────────────────────────────────────────────────────────

    val pdfCallback = remember { ShadeResultHolder() }

    val pdfLauncher =
        rememberDocumentLauncher(
            enabled = config.pdf != null,
            copyToCache = true,
            prefix = "PDF_",
            extensionProvider = { ".pdf" },
            context = context,
            callback = pdfCallback
        )

// ── Document picker ───────────────────────────────────────────────────────

    val documentCallback = remember { ShadeResultHolder() }

    val documentLauncher =
        rememberDocumentLauncher(
            enabled = config.document != null,
            copyToCache = config.document?.copyToCache == true,
            prefix = "DOC_",
            extensionProvider = {
                FileHelper.extensionFromUri(context, it)
            },
            context = context,
            callback = documentCallback
        )

    // ── Assemble handlers and build ComposeShadeCore ──────────────────────────

    return remember {
        val cameraHandler = ComposeCameraHandler(
            context = context,
            config = config,
            captureState = captureState,
            permCallbacks = permCallbacks,
            cameraPermLauncher = cameraPermLauncher,
            imageCameraLauncher = imageCameraLauncher,
            videoCameraLauncher = videoCameraLauncher,
            imageCameraCallback = imageCameraCallback,
            videoCameraCallback = videoCameraCallback,
        )

        val galleryHandler = ComposeGalleryHandler(
            context = context,
            config = config,
            permCallbacks = permCallbacks,
            mediaPermLauncher = mediaPermLauncher,
            imageGallerySingleLauncher = imageGallerySingleLauncher,
            imageGalleryMultiLauncher = imageGalleryMultiLauncher,
            videoGallerySingleLauncher = videoGallerySingleLauncher,
            videoGalleryMultiLauncher = videoGalleryMultiLauncher,
            imageGallerySingleCallback = imageGallerySingleCallback,
            imageGalleryMultiCallback = imageGalleryMultiCallback,
            videoGallerySingleCallback = videoGallerySingleCallback,
            videoGalleryMultiCallback = videoGalleryMultiCallback,
        )

        val documentHandler = ComposeDocumentHandler(
            config = config,
            pdfLauncher = pdfLauncher,
            documentLauncher = documentLauncher,
            pdfCallback = pdfCallback,
            documentCallback = documentCallback,
        )

        ComposeShadeCore(
            config = config,
            cameraHandler = cameraHandler,
            galleryHandler = galleryHandler,
            documentHandler = documentHandler,
        )
    }
}

