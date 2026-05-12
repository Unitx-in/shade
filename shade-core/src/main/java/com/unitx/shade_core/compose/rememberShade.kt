package com.unitx.shade_core.compose

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.unitx.shade_core.compose.core.ComposeShadeCore
import com.unitx.shade_core.compose.handler.ComposeCameraHandler
import com.unitx.shade_core.compose.handler.ComposeDocumentHandler
import com.unitx.shade_core.compose.handler.ComposeGalleryHandler
import com.unitx.shade_core.compose.state.CaptureState
import com.unitx.shade_core.compose.state.PermissionCallbackHolder
import com.unitx.shade_core.compose.state.ShadeResultHolder
import com.unitx.shade_core.common.config.ShadeConfig
import com.unitx.shade_core.core.ShadeCore

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
    val scope = rememberCoroutineScope()

// ── Permission launchers ──────────────────────────────────────────────────

    val cameraPermLauncher = rememberPermissionLauncher(
        enabled = config.image?.camera != null || config.video?.camera != null,
        permCallbacks = permCallbacks,
        which = { it.onCamera }  // read at callback time, not registration time
    )

    val mediaPermLauncher = rememberPermissionLauncher(
        enabled = config.video?.gallery != null, //  image gallery uses PickVisualMedia which doesn't need it.
        permCallbacks = permCallbacks,
        which = { it.onMedia }
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

    val imageGallerySingleLauncher = rememberSingleMediaLauncher(
        enabled = config.image?.gallery != null && config.image?.gallery?.multiSelect?.enabled != true,
        callback = imageGallerySingleCallback,
    )

// ── Image gallery multi ───────────────────────────────────────────────────

    val imageGalleryMultiCallback = remember { ShadeResultHolder() }

    val imageGalleryMultiLauncher = rememberMultiMediaLauncher(
        enabled = config.image?.gallery?.multiSelect?.enabled == true,
        maxItems = config.image?.gallery?.multiSelect?.maxItems ?: 2,
        callback = imageGalleryMultiCallback,
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

    val videoGallerySingleLauncher = rememberSingleMediaLauncher(
        enabled = config.video?.gallery != null && config.video?.gallery?.multiSelect?.enabled != true,
        callback = videoGallerySingleCallback,
    )

// ── Video gallery multi ───────────────────────────────────────────────────

    val videoGalleryMultiCallback = remember { ShadeResultHolder() }

    val videoGalleryMultiLauncher = rememberMultiMediaLauncher(
        enabled = config.video?.gallery?.multiSelect?.enabled == true,
        maxItems = config.video?.gallery?.multiSelect?.maxItems ?: 2,
        callback = videoGalleryMultiCallback,
    )

// ── Document picker Single ───────────────────────────────────────────────────────

    val documentSingleCallback = remember { ShadeResultHolder() }

    val documentSingleLauncher = rememberDocumentLauncher(
        enabled = config.document != null && config.document?.multiSelect?.enabled != true,
        callback = documentSingleCallback,
    )

    // ── Document picker multi ─────────────────────────────────────────────────

    val documentMultiCallback = remember { ShadeResultHolder() }

    val documentMultiLauncher = rememberMultiDocumentLauncher(
        enabled = config.document?.multiSelect?.enabled == true,
        callback = documentMultiCallback,
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
            scope = scope
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
            scope = scope
        )

        val documentHandler = ComposeDocumentHandler(
            context = context,
            config = config,
            scope = scope,
            documentSingleLauncher = documentSingleLauncher,
            documentMultiLauncher = documentMultiLauncher,
            documentSingleCallback = documentSingleCallback,
            documentMultiCallback = documentMultiCallback,
        )

        ComposeShadeCore(
            config = config,
            cameraHandler = cameraHandler,
            galleryHandler = galleryHandler,
            documentHandler = documentHandler,
        )
    }
}

