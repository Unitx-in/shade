package com.unitx.shade_core.compose

import android.Manifest
import android.app.Activity
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
import com.unitx.shade_core.common.CameraTarget
import com.unitx.shade_core.common.FileHelper
import com.unitx.shade_core.common.PermissionHelper
import com.unitx.shade_core.common.PickerHelper
import com.unitx.shade_core.common.DocumentMimeType
import com.unitx.shade_core.core.ShadeCore
import com.unitx.shade_core.config.ShadeConfig
import com.unitx.shade_core.result.ShadeError
import com.unitx.shade_core.result.ShadeResult

/**
 * Composable hook that creates and remembers a [ShadeCore] instance.
 *
 * **Only launchers for configured media types are registered.**
 * Each `rememberLauncherForActivityResult` call is gated on whether
 * the corresponding config block is present. Because [ShadeConfig] is
 * built once inside `remember { }` and never mutated, these conditions
 * are stable for the entire lifetime of the composition — they will
 * never flip between recompositions, satisfying Compose's real
 * constraint (no calls that change across recompositions).
 *
 * ```kotlin
 * // Only TakePicture + camera permission launchers are registered here.
 * val shade = rememberShade {
 *     image {
 *         camera {
 *             onResult { result -> }
 *             onFailure { error -> }
 *         }
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

    // ── Camera permission ─────────────────────────────────────────────────────
    // Registered only if image camera OR video camera is configured.

    val cameraPermLauncher: ActivityResultLauncher<String>? =
        if (config.image?.camera != null || config.video?.camera != null)
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                permCallbacks.onCamera?.invoke(granted)
            }
        else null

    // ── Media permission (video gallery, API 33+) ─────────────────────────────

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

    // ── Wire and build ────────────────────────────────────────────────────────

    val shade = remember {
        wireCallbacks(
            context = context,
            config = config,
            captureState = captureState,
            permCallbacks = permCallbacks,
            cameraPermLauncher = cameraPermLauncher,
            mediaPermLauncher = mediaPermLauncher,
            imageCameraCallback = imageCameraCallback,
            imageCameraLauncher = imageCameraLauncher,
            imageGallerySingleCallback = imageGallerySingleCallback,
            imageGallerySingleLauncher = imageGallerySingleLauncher,
            imageGalleryMultiCallback = imageGalleryMultiCallback,
            imageGalleryMultiLauncher = imageGalleryMultiLauncher,
            videoCameraCallback = videoCameraCallback,
            videoCameraLauncher = videoCameraLauncher,
            videoGallerySingleCallback = videoGallerySingleCallback,
            videoGallerySingleLauncher = videoGallerySingleLauncher,
            videoGalleryMultiCallback = videoGalleryMultiCallback,
            videoGalleryMultiLauncher = videoGalleryMultiLauncher,
            pdfCallback = pdfCallback,
            pdfLauncher = pdfLauncher,
            documentCallback = documentCallback,
            documentLauncher = documentLauncher,
        )
    }

    return shade
}

// =============================================================================
// Wiring
// =============================================================================

private fun wireCallbacks(
    context: Context,
    config: ShadeConfig,
    captureState: CaptureState,
    permCallbacks: PermissionCallbackHolder,
    cameraPermLauncher: ActivityResultLauncher<String>?,
    mediaPermLauncher: ActivityResultLauncher<String>?,
    imageCameraCallback: ShadeResultHolder,
    imageCameraLauncher: ActivityResultLauncher<Uri>?,
    imageGallerySingleCallback: ShadeResultHolder,
    imageGallerySingleLauncher: ActivityResultLauncher<PickVisualMediaRequest>?,
    imageGalleryMultiCallback: ShadeResultHolder,
    imageGalleryMultiLauncher: ActivityResultLauncher<PickVisualMediaRequest>?,
    videoCameraCallback: ShadeResultHolder,
    videoCameraLauncher: ActivityResultLauncher<Uri>?,
    videoGallerySingleCallback: ShadeResultHolder,
    videoGallerySingleLauncher: ActivityResultLauncher<PickVisualMediaRequest>?,
    videoGalleryMultiCallback: ShadeResultHolder,
    videoGalleryMultiLauncher: ActivityResultLauncher<PickVisualMediaRequest>?,
    pdfCallback: ShadeResultHolder,
    pdfLauncher: ActivityResultLauncher<Array<String>>?,
    documentCallback: ShadeResultHolder,
    documentLauncher: ActivityResultLauncher<Array<String>>?,
): ShadeCore {

    fun hasPermission(perm: String) = PermissionHelper.hasPermission(context, perm)

    fun shouldShowRationale(perm: String) =
        (context as? Activity)?.shouldShowRequestPermissionRationale(perm) ?: false

    // ── Route ShadeResult → DSL config callbacks ──────────────────────────────

    imageCameraCallback.onResult = { config.image?.camera?.onResult?.invoke(it as ShadeResult.Captured) }
    imageCameraCallback.onFailure = { config.image?.camera?.onFailure?.invoke(it) }

    imageGallerySingleCallback.onResult = { config.image?.gallery?.onResult?.invoke(it) }
    imageGallerySingleCallback.onFailure = { config.image?.gallery?.onFailure?.invoke(it) }

    imageGalleryMultiCallback.onResult = { config.image?.gallery?.onResult?.invoke(it) }
    imageGalleryMultiCallback.onFailure = { config.image?.gallery?.onFailure?.invoke(it) }

    videoCameraCallback.onResult =
        { config.video?.camera?.onResult?.invoke(it as ShadeResult.Captured) }
    videoCameraCallback.onFailure = { config.video?.camera?.onFailure?.invoke(it) }

    videoGallerySingleCallback.onResult = { config.video?.gallery?.onResult?.invoke(it) }
    videoGallerySingleCallback.onFailure = { config.video?.gallery?.onFailure?.invoke(it) }

    videoGalleryMultiCallback.onResult = { config.video?.gallery?.onResult?.invoke(it) }
    videoGalleryMultiCallback.onFailure = { config.video?.gallery?.onFailure?.invoke(it) }

    pdfCallback.onResult = { config.pdf?.onResult?.invoke(it as ShadeResult.Single) }
    pdfCallback.onFailure = { config.pdf?.onFailure?.invoke(it) }

    documentCallback.onResult = { config.document?.onResult?.invoke(it as ShadeResult.Single) }
    documentCallback.onFailure = { config.document?.onFailure?.invoke(it) }

    // ── Camera helpers ────────────────────────────────────────────────────────

    fun launchImageCamera() {
        val (file, uri) = FileHelper.createTempFile(context, "IMG_", ".jpg") ?: run {
            config.image?.camera?.onFailure?.invoke(ShadeError.FileCreationFailed); return
        }
        captureState.file = file
        captureState.uri = uri
        imageCameraLauncher?.launch(uri)
    }

    fun launchVideoCamera() {
        val (file, uri) = FileHelper.createTempFile(context, "VID_", ".mp4") ?: run {
            config.video?.camera?.onFailure?.invoke(ShadeError.FileCreationFailed); return
        }
        captureState.file = file
        captureState.uri = uri
        videoCameraLauncher?.launch(uri)
    }

    fun executeCamera(target: CameraTarget) = when (target) {
        CameraTarget.IMAGE -> launchImageCamera()
        CameraTarget.VIDEO -> launchVideoCamera()
    }

    // ── Permission callbacks ──────────────────────────────────────────────────

    var pendingTarget = CameraTarget.IMAGE

    permCallbacks.onCamera = { granted ->
        if (granted) {
            executeCamera(pendingTarget)
        } else {
            val error = if (shouldShowRationale(Manifest.permission.CAMERA))
                ShadeError.PermissionDenied else ShadeError.PermissionPermanentlyDenied
            when (pendingTarget) {
                CameraTarget.IMAGE -> config.image?.camera?.onFailure?.invoke(error)
                CameraTarget.VIDEO -> config.video?.camera?.onFailure?.invoke(error)
            }
        }
        pendingTarget = CameraTarget.IMAGE
    }

    permCallbacks.onMedia = { granted ->
        if (granted) {
            PickerHelper.launchVideoGalleryInternal(
                config,
                videoGallerySingleLauncher,
                videoGalleryMultiLauncher
            )
        } else {
            val perm = PermissionHelper.readVideoPermission()
            val error = if (shouldShowRationale(perm))
                ShadeError.PermissionDenied else ShadeError.PermissionPermanentlyDenied
            config.video?.gallery?.onFailure?.invoke(error)
        }
    }

    return ComposeShadeCore(
        config = config,
        hasPermissionFn = ::hasPermission,
        cameraPermLauncher = cameraPermLauncher,
        mediaPermLauncher = mediaPermLauncher,
        imageCameraLaunchFn = ::launchImageCamera,
        imageGallerySingleLauncher = imageGallerySingleLauncher,
        imageGalleryMultiLauncher = imageGalleryMultiLauncher,
        videoCameraLaunchFn = ::launchVideoCamera,
        videoGallerySingleLauncher = videoGallerySingleLauncher,
        videoGalleryMultiLauncher = videoGalleryMultiLauncher,
        pdfLauncher = pdfLauncher,
        documentLauncher = documentLauncher,
        defaultDocMimes = DocumentMimeType.ALL_VALUES,
        getPendingTarget = { pendingTarget },
        setPendingTarget = { pendingTarget = it },
    )
}

