package com.unitx.shade_core.compose

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.unitx.shade_core.action.ShadeAction
import com.unitx.shade_core.common.CameraTarget
import com.unitx.shade_core.common.FileHandler
import com.unitx.shade_core.core.DocumentMimeType
import com.unitx.shade_core.core.ShadeCore
import com.unitx.shade_core.dsl.ShadeConfig
import com.unitx.shade_core.result.ShadeError
import com.unitx.shade_core.result.ShadeResult
import java.io.File

/**
 * Composable hook that creates and remembers a [ShadeCore] instance
 * for the lifetime of the composition.
 *
 * All launchers are registered unconditionally (Compose does not allow
 * conditional composable calls), but [ComposeShadeCore.launch] guards
 * every dispatch with a config null-check — unconfigured launchers are
 * never actually fired.
 *
 * Raw Android results ([Boolean], [Uri?], [List<Uri>]) are converted to
 * [ShadeResult] immediately at each launcher boundary — [ShadeResult] is
 * the only type that flows through the rest of the wiring.
 *
 * The returned [ShadeCore] is stable across recompositions; call
 * [ShadeCore.launch] from any event handler.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun UploadScreen(viewModel: UploadViewModel = viewModel()) {
 *
 *     val shade = rememberShade {
 *
 *         image {
 *             camera {
 *                 onResult { result -> viewModel.onImageCaptured(result.file, result.uri) }
 *                 onFailure { error -> viewModel.onError(error) }
 *             }
 *             gallery {
 *                 multiSelect(maxItems = 5)
 *                 onResult { result ->
 *                     when (result) {
 *                         is ShadeResult.Single   -> viewModel.addImage(result.uri)
 *                         is ShadeResult.Multiple -> viewModel.addImages(result.uris)
 *                         else -> Unit
 *                     }
 *                 }
 *                 onFailure { error -> viewModel.onError(error) }
 *             }
 *         }
 *
 *         pdf {
 *             onResult { result -> viewModel.onPdfPicked(result.uri, result.file!!) }
 *             onFailure { error -> viewModel.onError(error) }
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

    // ── Permission launchers ──────────────────────────────────────────────────

    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permCallbacks.onCamera?.invoke(granted) }

    val mediaPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permCallbacks.onMedia?.invoke(granted) }

    // ── Image camera ──────────────────────────────────────────────────────────

    val imageCameraCallback = remember { ShadeResultHolder() }
    val imageCameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
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

    // ── Image gallery single ──────────────────────────────────────────────────

    val imageGallerySingleCallback = remember { ShadeResultHolder() }
    val imageGallerySingleLauncher = rememberLauncherForActivityResult(
        PickVisualMedia()
    ) { uri ->
        val result: ShadeResult? = uri?.let { ShadeResult.Single(it) }
        imageGallerySingleCallback.invoke(result, ShadeError.PickCancelled)
    }

    // ── Image gallery multi ───────────────────────────────────────────────────

    val imageGalleryMultiCallback = remember { ShadeResultHolder() }
    val imageGalleryMultiLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = config.image?.gallery?.maxItems?.coerceAtLeast(2) ?: 2
        )
    ) { uris ->
        val result: ShadeResult? = if (uris.isNotEmpty()) ShadeResult.Multiple(uris) else null
        imageGalleryMultiCallback.invoke(result, ShadeError.PickCancelled)
    }

    // ── Video camera ──────────────────────────────────────────────────────────

    val videoCameraCallback = remember { ShadeResultHolder() }
    val videoCameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { success ->
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

    // ── Video gallery single ──────────────────────────────────────────────────

    val videoGallerySingleCallback = remember { ShadeResultHolder() }
    val videoGallerySingleLauncher = rememberLauncherForActivityResult(
        PickVisualMedia()
    ) { uri ->
        val result: ShadeResult? = uri?.let { ShadeResult.Single(it) }
        videoGallerySingleCallback.invoke(result, ShadeError.PickCancelled)
    }

    // ── Video gallery multi ───────────────────────────────────────────────────

    val videoGalleryMultiCallback = remember { ShadeResultHolder() }
    val videoGalleryMultiLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = config.video?.gallery?.maxItems?.coerceAtLeast(2) ?: 2
        )
    ) { uris ->
        val result: ShadeResult? = if (uris.isNotEmpty()) ShadeResult.Multiple(uris) else null
        videoGalleryMultiCallback.invoke(result, ShadeError.PickCancelled)
    }

    // ── PDF picker ────────────────────────────────────────────────────────────

    val pdfCallback = remember { ShadeResultHolder() }
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val result: ShadeResult? = uri?.let {
            val file = FileHandler.copyUriToCache(context, it, "PDF_", ".pdf")
            if (file != null) ShadeResult.Single(it, file) else null
        }
        val error = if (uri != null && result == null) ShadeError.FileSaveFailed
        else ShadeError.PickCancelled
        pdfCallback.invoke(result, error)
    }

    // ── Document picker ───────────────────────────────────────────────────────

    val documentCallback = remember { ShadeResultHolder() }
    val documentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val docConfig = config.document
        val result: ShadeResult? = uri?.let {
            val file = if (docConfig?.copyToCache == true)
                FileHandler.copyUriToCache(
                    context,
                    it,
                    "DOC_",
                    FileHandler.extensionFromUri(context, it)
                )
            else null
            if (docConfig?.copyToCache == true && file == null) null
            else ShadeResult.Single(it, file)
        }
        val error = if (uri != null && result == null) ShadeError.FileSaveFailed
        else ShadeError.PickCancelled
        documentCallback.invoke(result, error)
    }

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
    cameraPermLauncher: ActivityResultLauncher<String>,
    mediaPermLauncher: ActivityResultLauncher<String>,
    imageCameraCallback: ShadeResultHolder,
    imageCameraLauncher: ActivityResultLauncher<Uri>,
    imageGallerySingleCallback: ShadeResultHolder,
    imageGallerySingleLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    imageGalleryMultiCallback: ShadeResultHolder,
    imageGalleryMultiLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    videoCameraCallback: ShadeResultHolder,
    videoCameraLauncher: ActivityResultLauncher<Uri>,
    videoGallerySingleCallback: ShadeResultHolder,
    videoGallerySingleLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    videoGalleryMultiCallback: ShadeResultHolder,
    videoGalleryMultiLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    pdfCallback: ShadeResultHolder,
    pdfLauncher: ActivityResultLauncher<Array<String>>,
    documentCallback: ShadeResultHolder,
    documentLauncher: ActivityResultLauncher<Array<String>>,
): ShadeCore {

    fun hasPermission(perm: String) =
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

    fun shouldShowRationale(perm: String) =
        (context as? Activity)?.shouldShowRequestPermissionRationale(perm) ?: false

    // ── Route ShadeResult → DSL config callbacks ──────────────────────────────

    imageCameraCallback.onResult =
        { config.image?.camera?.onResult?.invoke(it as ShadeResult.Captured) }
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
        val (file, uri) = FileHandler.createTempFile(context, "IMG_", ".jpg") ?: run {
            config.image?.camera?.onFailure?.invoke(ShadeError.FileCreationFailed); return
        }
        captureState.file = file
        captureState.uri = uri
        imageCameraLauncher.launch(uri)
    }

    fun launchVideoCamera() {
        val (file, uri) = FileHandler.createTempFile(context, "VID_", ".mp4") ?: run {
            config.video?.camera?.onFailure?.invoke(ShadeError.FileCreationFailed); return
        }
        captureState.file = file
        captureState.uri = uri
        videoCameraLauncher.launch(uri)
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
            launchVideoGalleryInternal(
                config,
                videoGallerySingleLauncher,
                videoGalleryMultiLauncher
            )
        } else {
            val perm = readVideoPermission()
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

// =============================================================================
// Helpers
// =============================================================================

internal fun launchVideoGalleryInternal(
    config: ShadeConfig,
    singleLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    multiLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
) {
    val g = config.video?.gallery ?: return
    if (g.isMultiSelect)
        multiLauncher.launch(PickVisualMediaRequest(PickVisualMedia.VideoOnly))
    else
        singleLauncher.launch(PickVisualMediaRequest(PickVisualMedia.VideoOnly))
}

internal fun readVideoPermission() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_VIDEO
    else
        Manifest.permission.READ_EXTERNAL_STORAGE

// =============================================================================
// Internal holders
// =============================================================================

internal class CaptureState {
    var file: File? = null
    var uri: Uri? = null
    fun clear() {
        file = null; uri = null
    }
}

internal class ShadeResultHolder {
    var onResult: ((ShadeResult) -> Unit)? = null
    var onFailure: ((ShadeError) -> Unit)? = null

    fun invoke(result: ShadeResult?, fallbackError: ShadeError) {
        if (result != null) onResult?.invoke(result)
        else onFailure?.invoke(fallbackError)
    }
}

internal class PermissionCallbackHolder {
    var onCamera: ((Boolean) -> Unit)? = null
    var onMedia: ((Boolean) -> Unit)? = null
}
