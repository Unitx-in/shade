package com.unitx.shade_core.compose

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
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
import com.unitx.shade_core.config.ShadeConfig
import com.unitx.shade_core.core.ShadeCore
import com.unitx.shade_core.result.ShadeError
import com.unitx.shade_core.result.ShadeResult

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

    val cameraPermLauncher: ActivityResultLauncher<String>? =
        if (config.image?.camera != null || config.video?.camera != null)
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                permCallbacks.onCamera?.invoke(granted)
            }
        else null

    val mediaPermLauncher: ActivityResultLauncher<String>? =
        if (config.video?.gallery != null)
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                permCallbacks.onMedia?.invoke(granted)
            }
        else null

    // ── Image camera ──────────────────────────────────────────────────────────

    val imageCameraCallback = remember { ShadeResultHolder() }
    val imageCameraLauncher: ActivityResultLauncher<Uri>? =
        if (config.image?.camera != null)
            rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                val file = captureState.file
                val uri = captureState.uri
                captureState.clear()
                val result: ShadeResult? = if (success && file != null && uri != null)
                    ShadeResult.Captured(file, uri)
                else {
                    file?.delete(); null
                }
                imageCameraCallback.invoke(result, ShadeError.CaptureFailed)
            }
        else null

    // ── Image gallery single ──────────────────────────────────────────────────

    val imageGallerySingleCallback = remember { ShadeResultHolder() }
    val imageGallerySingleLauncher: ActivityResultLauncher<PickVisualMediaRequest>? =
        if (config.image?.gallery != null && config.image?.gallery?.isMultiSelect == false)
            rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
                val result: ShadeResult? = uri?.let { ShadeResult.Single(it) }
                imageGallerySingleCallback.invoke(result, ShadeError.PickCancelled)
            }
        else null

    // ── Image gallery multi ───────────────────────────────────────────────────

    val imageGalleryMultiCallback = remember { ShadeResultHolder() }
    val imageGalleryMultiLauncher: ActivityResultLauncher<PickVisualMediaRequest>? =
        if (config.image?.gallery?.isMultiSelect == true)
            rememberLauncherForActivityResult(
                ActivityResultContracts.PickMultipleVisualMedia(
                    maxItems = config.image?.gallery?.maxItems?.coerceAtLeast(2) ?: 2
                )
            ) { uris ->
                val result: ShadeResult? =
                    if (uris.isNotEmpty()) ShadeResult.Multiple(uris) else null
                imageGalleryMultiCallback.invoke(result, ShadeError.PickCancelled)
            }
        else null

    // ── Video camera ──────────────────────────────────────────────────────────

    val videoCameraCallback = remember { ShadeResultHolder() }
    val videoCameraLauncher: ActivityResultLauncher<Uri>? =
        if (config.video?.camera != null)
            rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
                val file = captureState.file
                val uri = captureState.uri
                captureState.clear()
                val result: ShadeResult? = if (success && file != null && uri != null)
                    ShadeResult.Captured(file, uri)
                else {
                    file?.delete(); null
                }
                videoCameraCallback.invoke(result, ShadeError.CaptureFailed)
            }
        else null

    // ── Video gallery single ──────────────────────────────────────────────────

    val videoGallerySingleCallback = remember { ShadeResultHolder() }
    val videoGallerySingleLauncher: ActivityResultLauncher<PickVisualMediaRequest>? =
        if (config.video?.gallery != null && config.video?.gallery?.isMultiSelect == false)
            rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
                val result: ShadeResult? = uri?.let { ShadeResult.Single(it) }
                videoGallerySingleCallback.invoke(result, ShadeError.PickCancelled)
            }
        else null

    // ── Video gallery multi ───────────────────────────────────────────────────

    val videoGalleryMultiCallback = remember { ShadeResultHolder() }
    val videoGalleryMultiLauncher: ActivityResultLauncher<PickVisualMediaRequest>? =
        if (config.video?.gallery?.isMultiSelect == true)
            rememberLauncherForActivityResult(
                ActivityResultContracts.PickMultipleVisualMedia(
                    maxItems = config.video?.gallery?.maxItems?.coerceAtLeast(2) ?: 2
                )
            ) { uris ->
                val result: ShadeResult? =
                    if (uris.isNotEmpty()) ShadeResult.Multiple(uris) else null
                videoGalleryMultiCallback.invoke(result, ShadeError.PickCancelled)
            }
        else null

    // ── PDF picker ────────────────────────────────────────────────────────────

    val pdfCallback = remember { ShadeResultHolder() }
    val pdfLauncher: ActivityResultLauncher<Array<String>>? =
        if (config.pdf != null)
            rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                val result: ShadeResult? = uri?.let {
                    val file = FileHelper.copyUriToCache(context, it, "PDF_", ".pdf")
                    if (file != null) ShadeResult.Single(it, file) else null
                }
                val error = if (uri != null && result == null) ShadeError.FileSaveFailed
                else ShadeError.PickCancelled
                pdfCallback.invoke(result, error)
            }
        else null

    // ── Document picker ───────────────────────────────────────────────────────

    val documentCallback = remember { ShadeResultHolder() }
    val documentLauncher: ActivityResultLauncher<Array<String>>? =
        if (config.document != null)
            rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                val docConfig = config.document
                val result: ShadeResult? = uri?.let {
                    val file = if (docConfig?.copyToCache == true)
                        FileHelper.copyUriToCache(
                            context,
                            it,
                            "DOC_",
                            FileHelper.extensionFromUri(context, it)
                        )
                    else null
                    if (docConfig?.copyToCache == true && file == null) null
                    else ShadeResult.Single(it, file)
                }
                val error = if (uri != null && result == null) ShadeError.FileSaveFailed
                else ShadeError.PickCancelled
                documentCallback.invoke(result, error)
            }
        else null

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