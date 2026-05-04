package com.unitx.shade_core.compose

import android.Manifest
import android.app.Activity
import android.content.Context
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
import androidx.core.app.ActivityCompat
import com.unitx.shade_core.action.ShadeAction
import com.unitx.shade_core.core.ShadeCore
import com.unitx.shade_core.dsl.ShadeConfig
import com.unitx.shade_core.result.ShadeError
import com.unitx.shade_core.result.ShadeResult
import java.io.File

/**
 * Composable hook that creates and remembers a [ShadeCore] instance
 * for the lifetime of the composition.
 *
 * All activity result launchers are registered via
 * [rememberLauncherForActivityResult] — which means they survive
 * recomposition correctly and respect the Compose lifecycle.
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
 *                 onResult { result ->
 *                     viewModel.onImageCaptured(result.file, result.uri)
 *                 }
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
 *         video {
 *             camera {
 *                 onResult { result -> viewModel.onVideoRecorded(result.file, result.uri) }
 *                 onFailure { error -> viewModel.onError(error) }
 *             }
 *             gallery {
 *                 onResult { result -> viewModel.onVideoPicked((result as ShadeResult.Single).uri) }
 *                 onFailure { error -> viewModel.onError(error) }
 *             }
 *         }
 *
 *         pdf {
 *             onResult { result -> viewModel.onPdfPicked(result.uri, result.file!!) }
 *             onFailure { error -> viewModel.onError(error) }
 *         }
 *
 *         document {
 *             copyToCache = true
 *             onResult { result -> viewModel.onDocumentPicked(result.uri, result.file) }
 *             onFailure { error -> viewModel.onError(error) }
 *         }
 *     }
 *
 *     Column {
 *         Button(onClick = { shade.launch(ShadeAction.Image.Camera)  }) { Text("Camera")        }
 *         Button(onClick = { shade.launch(ShadeAction.Image.Gallery) }) { Text("Gallery")       }
 *         Button(onClick = { shade.launch(ShadeAction.Video.Camera)  }) { Text("Record video")  }
 *         Button(onClick = { shade.launch(ShadeAction.Video.Gallery) }) { Text("Pick video")    }
 *         Button(onClick = { shade.launch(ShadeAction.Pdf)           }) { Text("Pick PDF")      }
 *         Button(onClick = { shade.launch(ShadeAction.Document())    }) { Text("Pick document") }
 *     }
 * }
 * ```
 *
 * ## Host detection
 *
 * `rememberShade` works on both [androidx.activity.ComponentActivity] and
 * any [androidx.fragment.app.FragmentActivity] host — it resolves
 * `shouldShowRequestPermissionRationale` via [LocalContext] cast to [Activity].
 *
 * ## FileProvider (required for camera capture)
 *
 * Add to your **AndroidManifest.xml**:
 * ```xml
 * <provider
 *     android:name="androidx.core.content.FileProvider"
 *     android:authorities="${applicationId}.fileprovider"
 *     android:exported="false"
 *     android:grantUriPermissions="true">
 *     <meta-data
 *         android:name="android.support.FILE_PROVIDER_PATHS"
 *         android:resource="@xml/shade_file_paths" />
 * </provider>
 * ```
 */
@Composable
fun rememberShade(block: ShadeConfig.() -> Unit): ShadeCore {

    val context = LocalContext.current
    val config = remember(block) {
        ShadeConfig().apply(block)
    }

    // ── Permission launchers ──────────────────────────────────────────────────

    // Mutable holder used to forward permission results after the
    // ShadeCore is constructed (breaks the chicken-and-egg problem).
    val permissionCallbacks = remember { PermissionCallbackHolder() }

    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permissionCallbacks.onCamera?.invoke(granted) }

    val mediaPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permissionCallbacks.onMedia?.invoke(granted) }

    // ── Image camera ──────────────────────────────────────────────────────────

    val imageCameraCallbacks = remember { ResultCallbackHolder<Boolean>() }
    val imageCameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> imageCameraCallbacks.callback?.invoke(success) }

    // ── Image gallery single ──────────────────────────────────────────────────

    val imageGallerySingleCallbacks = remember { ResultCallbackHolder<Uri?>() }
    val imageGallerySingleLauncher = rememberLauncherForActivityResult(
        PickVisualMedia()
    ) { uri -> imageGallerySingleCallbacks.callback?.invoke(uri) }

    // ── Image gallery multi ───────────────────────────────────────────────────

    val imageGalleryMultiCallbacks = remember { ResultCallbackHolder<List<Uri>>() }
    val imageGalleryMultiLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = config.image?.gallery?.maxItems?.coerceAtLeast(2) ?: 2
        )
    ) { uris -> imageGalleryMultiCallbacks.callback?.invoke(uris) }

    // ── Video camera ──────────────────────────────────────────────────────────

    val videoCameraCallbacks = remember { ResultCallbackHolder<Boolean>() }
    val videoCameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { success -> videoCameraCallbacks.callback?.invoke(success) }

    // ── Video gallery single ──────────────────────────────────────────────────

    val videoGallerySingleCallbacks = remember { ResultCallbackHolder<Uri?>() }
    val videoGallerySingleLauncher = rememberLauncherForActivityResult(
        PickVisualMedia()
    ) { uri -> videoGallerySingleCallbacks.callback?.invoke(uri) }

    // ── Video gallery multi ───────────────────────────────────────────────────

    val videoGalleryMultiCallbacks = remember { ResultCallbackHolder<List<Uri>>() }
    val videoGalleryMultiLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = config.video?.gallery?.maxItems?.coerceAtLeast(2) ?: 2
        )
    ) { uris -> videoGalleryMultiCallbacks.callback?.invoke(uris) }

    // ── PDF picker ────────────────────────────────────────────────────────────

    val pdfCallbacks = remember { ResultCallbackHolder<Uri?>() }
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> pdfCallbacks.callback?.invoke(uri) }

    // ── Document picker ───────────────────────────────────────────────────────

    val documentCallbacks = remember { ResultCallbackHolder<Uri?>() }
    val documentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> documentCallbacks.callback?.invoke(uri) }

    // ── Build and remember ShadeCore ──────────────────────────────────────────

    val shade = remember {
        buildShadeCore(
            context = context,
            config = config,
            permissionCallbacks = permissionCallbacks,
            cameraPermLauncher = cameraPermLauncher,
            mediaPermLauncher = mediaPermLauncher,
            imageCameraCallbacks = imageCameraCallbacks,
            imageCameraLauncher = imageCameraLauncher,
            imageGallerySingleCallbacks = imageGallerySingleCallbacks,
            imageGallerySingleLauncher = imageGallerySingleLauncher,
            imageGalleryMultiCallbacks = imageGalleryMultiCallbacks,
            imageGalleryMultiLauncher = imageGalleryMultiLauncher,
            videoCameraCallbacks = videoCameraCallbacks,
            videoCameraLauncher = videoCameraLauncher,
            videoGallerySingleCallbacks = videoGallerySingleCallbacks,
            videoGallerySingleLauncher = videoGallerySingleLauncher,
            videoGalleryMultiCallbacks = videoGalleryMultiCallbacks,
            videoGalleryMultiLauncher = videoGalleryMultiLauncher,
            pdfCallbacks = pdfCallbacks,
            pdfLauncher = pdfLauncher,
            documentCallbacks = documentCallbacks,
            documentLauncher = documentLauncher,
        )
    }

    return shade
}

// =============================================================================
// Internal builder — wires all Compose launchers into a ShadeCore instance
// =============================================================================

private fun buildShadeCore(
    context: Context,
    config: ShadeConfig,
    permissionCallbacks: PermissionCallbackHolder,
    cameraPermLauncher: ActivityResultLauncher<String>,
    mediaPermLauncher: ActivityResultLauncher<String>,
    imageCameraCallbacks: ResultCallbackHolder<Boolean>,
    imageCameraLauncher: ActivityResultLauncher<Uri>,
    imageGallerySingleCallbacks: ResultCallbackHolder<Uri?>,
    imageGallerySingleLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    imageGalleryMultiCallbacks: ResultCallbackHolder<List<Uri>>,
    imageGalleryMultiLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    videoCameraCallbacks: ResultCallbackHolder<Boolean>,
    videoCameraLauncher: ActivityResultLauncher<Uri>,
    videoGallerySingleCallbacks: ResultCallbackHolder<Uri?>,
    videoGallerySingleLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    videoGalleryMultiCallbacks: ResultCallbackHolder<List<Uri>>,
    videoGalleryMultiLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    pdfCallbacks: ResultCallbackHolder<Uri?>,
    pdfLauncher: ActivityResultLauncher<Array<String>>,
    documentCallbacks: ResultCallbackHolder<Uri?>,
    documentLauncher: ActivityResultLauncher<Array<String>>,
): ShadeCore {

    // Temp capture state lives here, outside ShadeCore, because Compose
    // launchers are invoked via closures rather than registered contracts.
    var tempCaptureFile: File? = null
    var tempCaptureUri: Uri? = null

    val defaultDocMimes = arrayOf(
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "text/plain",
        "text/csv",
        "application/rtf"
    )

    fun createTempFile(prefix: String, ext: String): Pair<File, Uri>? = try {
        val file = File.createTempFile(prefix, ext, context.cacheDir)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        Pair(file, uri)
    } catch (e: Exception) {
        null
    }

    fun copyUriToCache(uri: Uri, prefix: String, ext: String): File? = try {
        val file = File.createTempFile(prefix, ext, context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { i ->
            file.outputStream().use { o -> i.copyTo(o) }
        }
        file
    } catch (e: Exception) {
        null
    }

    fun extensionFromUri(uri: Uri): String {
        val mime = context.contentResolver.getType(uri) ?: return ".bin"
        return when {
            mime.contains("pdf") -> ".pdf"
            mime.contains("msword") -> ".doc"
            mime.contains("wordprocess") -> ".docx"
            mime.contains("ms-excel") -> ".xls"
            mime.contains("spreadsheet") -> ".xlsx"
            mime.contains("powerpoint") -> ".ppt"
            mime.contains("presentati") -> ".pptx"
            mime.contains("text/plain") -> ".txt"
            mime.contains("text/csv") -> ".csv"
            mime.contains("rtf") -> ".rtf"
            else -> ".bin"
        }
    }

    fun hasPermission(perm: String) =
        androidx.core.content.ContextCompat.checkSelfPermission(context, perm) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED

    fun rationaleChecker(perm: String) =
        (context as? Activity)?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, perm) } ?: false

    // ── Wire image camera ─────────────────────────────────────────────────────

    imageCameraCallbacks.callback = { success ->
        val file = tempCaptureFile
        val uri = tempCaptureUri
        tempCaptureFile = null; tempCaptureUri = null
        if (success && file != null && uri != null) {
            config.image?.camera?.onResult?.invoke(ShadeResult.Captured(file, uri))
        } else {
            file?.delete()
            config.image?.camera?.onFailure?.invoke(ShadeError.CaptureFailed)
        }
    }

    // ── Wire image gallery ────────────────────────────────────────────────────

    imageGallerySingleCallbacks.callback = { uri ->
        if (uri != null) config.image?.gallery?.onResult?.invoke(ShadeResult.Single(uri))
        else config.image?.gallery?.onFailure?.invoke(ShadeError.PickCancelled)
    }

    imageGalleryMultiCallbacks.callback = { uris ->
        if (uris.isNotEmpty()) config.image?.gallery?.onResult?.invoke(ShadeResult.Multiple(uris))
        else config.image?.gallery?.onFailure?.invoke(ShadeError.PickCancelled)
    }

    // ── Wire video camera ─────────────────────────────────────────────────────

    videoCameraCallbacks.callback = { success ->
        val file = tempCaptureFile
        val uri = tempCaptureUri
        tempCaptureFile = null; tempCaptureUri = null
        if (success && file != null && uri != null) {
            config.video?.camera?.onResult?.invoke(ShadeResult.Captured(file, uri))
        } else {
            file?.delete()
            config.video?.camera?.onFailure?.invoke(ShadeError.CaptureFailed)
        }
    }

    // ── Wire video gallery ────────────────────────────────────────────────────

    videoGallerySingleCallbacks.callback = { uri ->
        if (uri != null) config.video?.gallery?.onResult?.invoke(ShadeResult.Single(uri))
        else config.video?.gallery?.onFailure?.invoke(ShadeError.PickCancelled)
    }

    videoGalleryMultiCallbacks.callback = { uris ->
        if (uris.isNotEmpty()) config.video?.gallery?.onResult?.invoke(ShadeResult.Multiple(uris))
        else config.video?.gallery?.onFailure?.invoke(ShadeError.PickCancelled)
    }

    // ── Wire PDF ──────────────────────────────────────────────────────────────

    pdfCallbacks.callback = { uri ->
        if (uri == null) {
            config.pdf?.onFailure?.invoke(ShadeError.PickCancelled)
        } else {
            val file = copyUriToCache(uri, "PDF_", ".pdf")
            if (file != null) config.pdf?.onResult?.invoke(ShadeResult.Single(uri, file))
            else config.pdf?.onFailure?.invoke(ShadeError.FileSaveFailed)
        }
    }

    // ── Wire document ─────────────────────────────────────────────────────────

    documentCallbacks.callback = { uri ->
        val docConfig = config.document
        if (uri == null) {
            docConfig?.onFailure?.invoke(ShadeError.PickCancelled)
        } else {
            val file = if (docConfig?.copyToCache == true)
                copyUriToCache(uri, "DOC_", extensionFromUri(uri))
            else null
            if (docConfig?.copyToCache == true && file == null)
                docConfig.onFailure?.invoke(ShadeError.FileSaveFailed)
            else
                docConfig?.onResult?.invoke(ShadeResult.Single(uri, file))
        }
    }

    // ── Wire permission callbacks ─────────────────────────────────────────────

    var pendingCameraTarget = CameraTarget.IMAGE

    fun launchImageCamera() {
        val (file, uri) = createTempFile("IMG_", ".jpg") ?: run {
            config.image?.camera?.onFailure?.invoke(ShadeError.FileCreationFailed); return
        }
        tempCaptureFile = file; tempCaptureUri = uri
        imageCameraLauncher.launch(uri)
    }

    fun launchVideoCamera() {
        val (file, uri) = createTempFile("VID_", ".mp4") ?: run {
            config.video?.camera?.onFailure?.invoke(ShadeError.FileCreationFailed); return
        }
        tempCaptureFile = file; tempCaptureUri = uri
        videoCameraLauncher.launch(uri)
    }

    fun executeCamera(target: CameraTarget) = when (target) {
        CameraTarget.IMAGE -> launchImageCamera()
        CameraTarget.VIDEO -> launchVideoCamera()
    }

    permissionCallbacks.onCamera = { granted ->
        if (granted) {
            executeCamera(pendingCameraTarget)
        } else {
            val error = if (rationaleChecker(Manifest.permission.CAMERA))
                ShadeError.PermissionDenied else ShadeError.PermissionPermanentlyDenied
            when (pendingCameraTarget) {
                CameraTarget.IMAGE -> config.image?.camera?.onFailure?.invoke(error)
                CameraTarget.VIDEO -> config.video?.camera?.onFailure?.invoke(error)
            }
        }
        pendingCameraTarget = CameraTarget.IMAGE
    }

    permissionCallbacks.onMedia = { granted ->
        if (granted) {
            launchVideoGalleryInternal(
                config, videoGallerySingleLauncher, videoGalleryMultiLauncher
            )
        } else {
            val perm = readVideoPermission()
            val error = if (rationaleChecker(perm))
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
        defaultDocMimes = defaultDocMimes,
        pendingCameraTargetRef = { pendingCameraTarget },
        setPendingCameraTarget = { pendingCameraTarget = it },
    )
}


private fun launchVideoGalleryInternal(
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

private fun readVideoPermission() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_VIDEO
    else Manifest.permission.READ_EXTERNAL_STORAGE




