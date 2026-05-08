package com.unitx.shade_core.core

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import com.unitx.shade_core.config.ShadeConfig
import com.unitx.shade_core.registrar.ShadeRegistrar

/**
 * Registers every [ActivityResultContract] via [ShadeRegistrar] and holds
 * the resulting launchers. Result callbacks are injected after construction
 * via the [on*] properties so that [com.unitx.shade_core.handler.CameraHandler], [com.unitx.shade_core.handler.GalleryHandler], and
 * [com.unitx.shade_core.handler.DocumentHandler] can own the result logic without knowing about registration.
 */
internal class LauncherRegistry(
    private val registrar: ShadeRegistrar,
    private val config: ShadeConfig
) {

    // ── Callback slots — filled by each handler after construction ────────────

    var onCameraPermissionResult: ((Boolean) -> Unit)? = null
    var onMediaPermissionResult: ((Boolean) -> Unit)? = null
    var onImageCameraResult: ((Boolean) -> Unit)? = null
    var onImageGallerySingle: ((Uri?) -> Unit)? = null
    var onImageGalleryMulti: ((List<Uri>) -> Unit)? = null
    var onVideoCameraResult: ((Boolean) -> Unit)? = null
    var onVideoGallerySingle: ((Uri?) -> Unit)? = null
    var onVideoGalleryMulti: ((List<Uri>) -> Unit)? = null
    var onPdfResult: ((Uri?) -> Unit)? = null
    var onDocumentResult: ((Uri?) -> Unit)? = null

    // ── Permission launchers ──────────────────────────────────────────────────

    val cameraPermissionLauncher by lazy {
        registrar.register(ActivityResultContracts.RequestPermission()) { granted ->
            onCameraPermissionResult?.invoke(granted)
        }
    }

    val mediaPermissionLauncher by lazy {
        registrar.register(ActivityResultContracts.RequestPermission()) { granted ->
            onMediaPermissionResult?.invoke(granted)
        }
    }

    // ── Image camera ──────────────────────────────────────────────────────────

    val imageCameraLauncher by lazy {
        registrar.register(ActivityResultContracts.TakePicture()) { success ->
            onImageCameraResult?.invoke(success)
        }
    }

    // ── Image gallery ─────────────────────────────────────────────────────────

    val imageGallerySingleLauncher by lazy {
        registrar.register(PickVisualMedia()) { uri ->
            onImageGallerySingle?.invoke(uri)
        }
    }

    val imageGalleryMultiLauncher by lazy {
        registrar.register(
            ActivityResultContracts.PickMultipleVisualMedia(
                maxItems = config.image?.gallery?.maxItems?.coerceAtLeast(2) ?: 2
            )
        ) { uris ->
            onImageGalleryMulti?.invoke(uris)
        }
    }

    // ── Video camera ──────────────────────────────────────────────────────────

    val videoCameraLauncher by lazy {
        registrar.register(ActivityResultContracts.CaptureVideo()) { success ->
            onVideoCameraResult?.invoke(success)
        }
    }

    // ── Video gallery ─────────────────────────────────────────────────────────

    val videoGallerySingleLauncher by lazy {
        registrar.register(PickVisualMedia()) { uri ->
            onVideoGallerySingle?.invoke(uri)
        }
    }

    val videoGalleryMultiLauncher by lazy {
        registrar.register(
            ActivityResultContracts.PickMultipleVisualMedia(
                maxItems = config.video?.gallery?.maxItems?.coerceAtLeast(2) ?: 2
            )
        ) { uris ->
            onVideoGalleryMulti?.invoke(uris)
        }
    }

    // ── PDF picker ────────────────────────────────────────────────────────────

    val pdfPickerLauncher by lazy {
        registrar.register(ActivityResultContracts.OpenDocument()) { uri ->
            onPdfResult?.invoke(uri)
        }
    }

    // ── Document picker ───────────────────────────────────────────────────────

    val documentPickerLauncher by lazy {
        registrar.register(ActivityResultContracts.OpenDocument()) { uri ->
            onDocumentResult?.invoke(uri)
        }
    }

    // ── Eager initialisation — must be called before host reaches STARTED ─────

    fun registerAll() {
        cameraPermissionLauncher
        mediaPermissionLauncher
        imageCameraLauncher
        imageGallerySingleLauncher
        imageGalleryMultiLauncher
        videoCameraLauncher
        videoGallerySingleLauncher
        videoGalleryMultiLauncher
        pdfPickerLauncher
        documentPickerLauncher
    }
}