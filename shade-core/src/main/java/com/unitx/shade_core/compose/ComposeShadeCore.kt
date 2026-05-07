package com.unitx.shade_core.compose

import android.Manifest
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import com.unitx.shade_core.action.ShadeAction
import com.unitx.shade_core.common.CameraTarget
import com.unitx.shade_core.common.PermissionHelper
import com.unitx.shade_core.common.PickerHelper
import com.unitx.shade_core.core.ShadeCore
import com.unitx.shade_core.config.ShadeConfig
import com.unitx.shade_core.result.ShadeError

/**
 * [ShadeCore] variant for Compose. All launchers are nullable — only
 * launchers for configured media types will be non-null. Every dispatch
 * in [launch] checks non-null before calling, surfacing
 * [ShadeError.NotConfigured] via [onFailure] if the type was not set up.
 */
internal class ComposeShadeCore(
    private val config: ShadeConfig,
    private val hasPermissionFn: (String) -> Boolean,
    private val cameraPermLauncher: ActivityResultLauncher<String>?,
    private val mediaPermLauncher: ActivityResultLauncher<String>?,
    private val imageCameraLaunchFn: () -> Unit,
    private val imageGallerySingleLauncher: ActivityResultLauncher<PickVisualMediaRequest>?,
    private val imageGalleryMultiLauncher: ActivityResultLauncher<PickVisualMediaRequest>?,
    private val videoCameraLaunchFn: () -> Unit,
    private val videoGallerySingleLauncher: ActivityResultLauncher<PickVisualMediaRequest>?,
    private val videoGalleryMultiLauncher: ActivityResultLauncher<PickVisualMediaRequest>?,
    private val pdfLauncher: ActivityResultLauncher<Array<String>>?,
    private val documentLauncher: ActivityResultLauncher<Array<String>>?,
    private val defaultDocMimes: Array<String>,
    private val getPendingTarget: () -> CameraTarget,
    private val setPendingTarget: (CameraTarget) -> Unit,
) : ShadeCore(registrar = NoOpRegistrar, config = config) {

    override fun launch(action: ShadeAction) {
        when (action) {
            is ShadeAction.Image.Camera -> requireCamera(CameraTarget.IMAGE)
            is ShadeAction.Image.Gallery -> launchImageGallery()
            is ShadeAction.Video.Camera -> requireCamera(CameraTarget.VIDEO)
            is ShadeAction.Video.Gallery -> launchVideoGallery()
            is ShadeAction.Pdf -> launchPdf()
            is ShadeAction.Document -> launchDocument(action)
        }
    }

    override fun registerLaunchers() {}

    private fun requireCamera(target: CameraTarget) {
        // Launcher is null only if the config block was absent — surface the error.
        if (cameraPermLauncher == null) {
            when (target) {
                CameraTarget.IMAGE -> config.image?.camera?.onFailure?.invoke(ShadeError.NotConfigured)
                CameraTarget.VIDEO -> config.video?.camera?.onFailure?.invoke(ShadeError.NotConfigured)
            }
            return
        }
        setPendingTarget(target)
        if (hasPermissionFn(Manifest.permission.CAMERA)) {
            when (target) {
                CameraTarget.IMAGE -> imageCameraLaunchFn()
                CameraTarget.VIDEO -> videoCameraLaunchFn()
            }
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchImageGallery() {
        val g = config.image?.gallery ?: run {
            config.image?.gallery?.onFailure?.invoke(ShadeError.NotConfigured); return
        }
        val launcher =
            if (g.isMultiSelect) imageGalleryMultiLauncher else imageGallerySingleLauncher
        if (launcher == null) {
            config.image?.gallery?.onFailure?.invoke(ShadeError.NotConfigured); return
        }
        launcher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
    }

    private fun launchVideoGallery() {
        if (config.video?.gallery == null) {
            config.video?.gallery?.onFailure?.invoke(ShadeError.NotConfigured); return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perm = PermissionHelper.readVideoPermission()
            if (!hasPermissionFn(perm)) {
                mediaPermLauncher?.launch(perm); return
            }
        }
        PickerHelper.launchVideoGalleryInternal(config, videoGallerySingleLauncher, videoGalleryMultiLauncher)
    }

    private fun launchPdf() {
        if (pdfLauncher == null) {
            config.pdf?.onFailure?.invoke(ShadeError.NotConfigured); return
        }
        pdfLauncher.launch(arrayOf("application/pdf"))
    }

    private fun launchDocument(action: ShadeAction.Document) {
        if (documentLauncher == null) {
            config.document?.onFailure?.invoke(ShadeError.NotConfigured); return
        }
        val mimes = action.mimeTypes.takeIf { it.isNotEmpty() }?.toTypedArray() ?: defaultDocMimes
        documentLauncher.launch(mimes)
    }
}