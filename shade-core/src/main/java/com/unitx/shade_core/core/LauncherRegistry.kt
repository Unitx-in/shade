package com.unitx.shade_core.core

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import com.unitx.shade_core.common.config.ShadeConfig
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

    var onDocumentSingleResult: ((Uri?) -> Unit)? = null
    var onDocumentMultiResult: ((List<Uri>) -> Unit)? = null

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
                maxItems = config.image?.gallery?.multiSelect?.maxItems?.coerceAtLeast(2) ?: 2
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
                maxItems = config.video?.gallery?.multiSelect?.maxItems?.coerceAtLeast(2) ?: 2
            )
        ) { uris ->
            onVideoGalleryMulti?.invoke(uris)
        }
    }

    // ── Document picker ───────────────────────────────────────────────────────

    val documentSingleLauncher by lazy {
        registrar.register(ActivityResultContracts.OpenDocument()) { uri ->
            onDocumentSingleResult?.invoke(uri)
        }
    }

    val documentMultiLauncher by lazy {
        registrar.register(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            onDocumentMultiResult?.invoke(uris)
        }
    }

    fun registerConfigured() {

        val imageConfig = config.image
        val videoConfig = config.video

        val hasCamera = imageConfig?.camera != null || videoConfig?.camera != null
        val hasGallery = imageConfig?.gallery != null || videoConfig?.gallery != null

        if (hasCamera) cameraPermissionLauncher
        if (hasGallery) mediaPermissionLauncher

        imageConfig?.camera?.let {
            imageCameraLauncher
        }

        imageConfig?.gallery?.let { gallery ->
            if (gallery.multiSelect?.enabled == true) imageGalleryMultiLauncher
            else imageGallerySingleLauncher
        }

        videoConfig?.camera?.let {
            videoCameraLauncher
        }

        videoConfig?.gallery?.let { gallery ->
            if (gallery.multiSelect?.enabled == true) videoGalleryMultiLauncher
            else videoGallerySingleLauncher
        }

        config.document?.let { doc ->
            if (doc.multiSelect?.enabled == true) documentMultiLauncher
            else documentSingleLauncher
        }
    }
}