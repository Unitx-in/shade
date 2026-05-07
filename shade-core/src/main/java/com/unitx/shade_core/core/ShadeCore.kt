package com.unitx.shade_core.core

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.unitx.shade_core.action.ShadeAction
import com.unitx.shade_core.common.CameraTarget
import com.unitx.shade_core.common.FileHandler
import com.unitx.shade_core.dsl.ShadeConfig
import com.unitx.shade_core.registrar.ShadeRegistrar
import com.unitx.shade_core.result.ShadeError
import com.unitx.shade_core.result.ShadeResult
import java.io.File
import java.io.IOException

/**
 * Core engine. Do not instantiate directly — use [Shade.with] (XML/Fragment)
 * or [rememberShade] (Compose).
 *
 * All launchers are registered eagerly via [com.unitx.shade_core.registrar.ShadeRegistrar] so this class
 * has no knowledge of Fragment or Activity, making it host-agnostic.
 */
open class ShadeCore(
    private val registrar: ShadeRegistrar,
    private val config: ShadeConfig
) {

    init {
        registerLaunchers()
    }

    protected open fun registerLaunchers() {
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

    private val context get() = registrar.context

    // ─── Temp capture state ───────────────────────────────────────────────────

    private var tempCaptureFile: File? = null
    private var tempCaptureUri: Uri? = null

    // =========================================================================
    // Permission launchers
    // =========================================================================

    private val cameraPermissionLauncher by lazy {
        registrar.register(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                executeCamera(pendingCameraTarget)
            } else {
                val error =
                    if (registrar.shouldShowRationale(Manifest.permission.CAMERA))
                        ShadeError.PermissionDenied
                    else
                        ShadeError.PermissionPermanentlyDenied

                when (pendingCameraTarget) {
                    CameraTarget.IMAGE -> config.image?.camera?.onFailure?.invoke(error)
                    CameraTarget.VIDEO -> config.video?.camera?.onFailure?.invoke(error)
                }
            }
            pendingCameraTarget = CameraTarget.IMAGE
        }
    }

    private val mediaPermissionLauncher by lazy {
        registrar.register(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                launchVideoGallery()
            } else {
                val perm = readVideoPermission()
                val error =
                    if (registrar.shouldShowRationale(perm))
                        ShadeError.PermissionDenied
                    else
                        ShadeError.PermissionPermanentlyDenied

                config.video?.gallery?.onFailure?.invoke(error)
            }
        }
    }
    // =========================================================================
    // Activity result launchers
    // =========================================================================

    // ── Image camera ──────────────────────────────────────────────────────────

    private val imageCameraLauncher by lazy {
        registrar.register(
            ActivityResultContracts.TakePicture()
        ) { success ->
            val file = tempCaptureFile
            val uri = tempCaptureUri
            tempCaptureFile = null
            tempCaptureUri = null

            if (success && file != null && uri != null) {
                config.image?.camera?.onResult?.invoke(ShadeResult.Captured(file, uri))
            } else {
                file?.delete()
                config.image?.camera?.onFailure?.invoke(ShadeError.CaptureFailed)
            }
        }
    }

    // ── Image gallery — single ────────────────────────────────────────────────

    private val imageGallerySingleLauncher by lazy {
        registrar.register(
            PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                config.image?.gallery?.onResult?.invoke(ShadeResult.Single(uri))
            } else {
                config.image?.gallery?.onFailure?.invoke(ShadeError.PickCancelled)
            }
        }
    }

    // ── Image gallery — multi ─────────────────────────────────────────────────

    private val imageGalleryMultiLauncher by lazy {
        registrar.register(
            ActivityResultContracts.PickMultipleVisualMedia(
                maxItems = config.image?.gallery?.maxItems?.coerceAtLeast(2) ?: 2
            )
        ) { uris ->
            if (uris.isNotEmpty()) {
                config.image?.gallery?.onResult?.invoke(ShadeResult.Multiple(uris))
            } else {
                config.image?.gallery?.onFailure?.invoke(ShadeError.PickCancelled)
            }
        }
    }

    // ── Video camera ──────────────────────────────────────────────────────────

    private val videoCameraLauncher by lazy {
        registrar.register(
            ActivityResultContracts.CaptureVideo()
        ) { success ->
            val file = tempCaptureFile
            val uri = tempCaptureUri
            tempCaptureFile = null
            tempCaptureUri = null

            if (success && file != null && uri != null) {
                config.video?.camera?.onResult?.invoke(ShadeResult.Captured(file, uri))
            } else {
                file?.delete()
                config.video?.camera?.onFailure?.invoke(ShadeError.CaptureFailed)
            }
        }
    }

    // ── Video gallery — single ────────────────────────────────────────────────

    private val videoGallerySingleLauncher by lazy {
        registrar.register(
            PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                config.video?.gallery?.onResult?.invoke(ShadeResult.Single(uri))
            } else {
                config.video?.gallery?.onFailure?.invoke(ShadeError.PickCancelled)
            }
        }
    }

    // ── Video gallery — multi ─────────────────────────────────────────────────

    private val videoGalleryMultiLauncher by lazy {
        registrar.register(
            ActivityResultContracts.PickMultipleVisualMedia(
                maxItems = config.video?.gallery?.maxItems?.coerceAtLeast(2) ?: 2
            )
        ) { uris ->
            if (uris.isNotEmpty()) {
                config.video?.gallery?.onResult?.invoke(ShadeResult.Multiple(uris))
            } else {
                config.video?.gallery?.onFailure?.invoke(ShadeError.PickCancelled)
            }
        }
    }

    // ── PDF picker ────────────────────────────────────────────────────────────

    private val pdfPickerLauncher by lazy {
        registrar.register(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri == null) {
                config.pdf?.onFailure?.invoke(ShadeError.PickCancelled)
                return@register
            }

            val file = FileHandler.copyUriToCache(context,uri, "PDF_", ".pdf")
            if (file != null) {
                config.pdf?.onResult?.invoke(ShadeResult.Single(uri, file))
            } else {
                config.pdf?.onFailure?.invoke(ShadeError.FileSaveFailed)
            }
        }
    }

    // ── Document picker ───────────────────────────────────────────────────────

    private val documentPickerLauncher by lazy {
        registrar.register(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            val docConfig = config.document

            if (uri == null) {
                docConfig?.onFailure?.invoke(ShadeError.PickCancelled)
                return@register
            }

            val file =
                if (docConfig?.copyToCache == true)
                    FileHandler.copyUriToCache(context,uri, "DOC_", FileHandler.extensionFromUri(context,uri))
                else null

            if (docConfig?.copyToCache == true && file == null) {
                docConfig.onFailure?.invoke(ShadeError.FileSaveFailed)
            } else {
                docConfig?.onResult?.invoke(ShadeResult.Single(uri, file))
            }
        }
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Dispatch a [ShadeAction]. Call this from click handlers or Compose
     * event callbacks.
     *
     * ```kotlin
     * // XML
     * binding.btnCamera.setOnClickListener { shade.launch(ShadeAction.Image.Camera) }
     *
     * // Compose
     * Button(onClick = { shade.launch(ShadeAction.Image.Camera) }) { Text("Camera") }
     * ```
     */
    open fun launch(action: ShadeAction) {
        when (action) {
            is ShadeAction.Image.Camera -> handleImageCamera()
            is ShadeAction.Image.Gallery -> handleImageGallery()
            is ShadeAction.Video.Camera -> handleVideoCamera()
            is ShadeAction.Video.Gallery -> handleVideoGallery()
            is ShadeAction.Pdf -> handlePdf()
            is ShadeAction.Document -> handleDocument(action)
        }
    }

    // =========================================================================
    // Handlers
    // =========================================================================

    private fun handleImageCamera() {
        if (config.image?.camera == null) {
            config.image?.camera?.onFailure?.invoke(ShadeError.NotConfigured)
            return
        }
        requireCameraPermission(CameraTarget.IMAGE)
    }

    private fun handleImageGallery() {
        val galleryConfig = config.image?.gallery ?: return
        if (galleryConfig.isMultiSelect) {
            imageGalleryMultiLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        } else {
            imageGallerySingleLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        }
    }

    private fun handleVideoCamera() {
        if (config.video?.camera == null) {
            config.video?.camera?.onFailure?.invoke(ShadeError.NotConfigured)
            return
        }
        requireCameraPermission(CameraTarget.VIDEO)
    }

    private fun handleVideoGallery() {
        if (config.video?.gallery == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perm = readVideoPermission()
            if (!hasPermission(perm)) {
                mediaPermissionLauncher.launch(perm)
                return
            }
        }
        launchVideoGallery()
    }

    private fun handlePdf() {
        if (config.pdf == null) {
            config.pdf?.onFailure?.invoke(ShadeError.NotConfigured)
            return
        }
        pdfPickerLauncher.launch(arrayOf("application/pdf"))
    }

    private fun handleDocument(action: ShadeAction.Document) {
        if (config.document == null) {
            config.document?.onFailure?.invoke(ShadeError.NotConfigured)
            return
        }
        val mimeTypes = action.mimeTypes
            .takeIf { it.isNotEmpty() }
            ?.toTypedArray()
            ?: DocumentMimeType.ALL_VALUES
        documentPickerLauncher.launch(mimeTypes)
    }

    // =========================================================================
    // Camera internals
    // =========================================================================

    private var pendingCameraTarget: CameraTarget = CameraTarget.IMAGE

    private fun requireCameraPermission(target: CameraTarget) {
        pendingCameraTarget = target
        if (hasPermission(Manifest.permission.CAMERA)) {
            executeCamera(target)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun executeCamera(target: CameraTarget) {
        when (target) {
            CameraTarget.IMAGE -> launchImageCamera()
            CameraTarget.VIDEO -> launchVideoCamera()
        }
    }

    private fun launchImageCamera() {
        val (file, uri) = FileHandler.createTempFile(context,"IMG_", ".jpg") ?: run {
            config.image?.camera?.onFailure?.invoke(ShadeError.FileCreationFailed)
            return
        }
        tempCaptureFile = file
        tempCaptureUri = uri
        imageCameraLauncher.launch(uri)
    }

    private fun launchVideoCamera() {
        val (file, uri) = FileHandler.createTempFile(context,"VID_", ".mp4") ?: run {
            config.video?.camera?.onFailure?.invoke(ShadeError.FileCreationFailed)
            return
        }
        tempCaptureFile = file
        tempCaptureUri = uri
        videoCameraLauncher.launch(uri)
    }

    private fun launchVideoGallery() {
        val galleryConfig = config.video?.gallery ?: return
        if (galleryConfig.isMultiSelect) {
            videoGalleryMultiLauncher.launch(PickVisualMediaRequest(PickVisualMedia.VideoOnly))
        } else {
            videoGallerySingleLauncher.launch(PickVisualMediaRequest(PickVisualMedia.VideoOnly))
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================


    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun readVideoPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_VIDEO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE
}