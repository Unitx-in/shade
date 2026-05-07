package com.unitx.shade_core.compose

import android.Manifest
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import com.unitx.shade_core.action.ShadeAction
import com.unitx.shade_core.common.CameraTarget
import com.unitx.shade_core.core.ShadeCore
import com.unitx.shade_core.dsl.ShadeConfig
import com.unitx.shade_core.result.ShadeError


/**
 * [ShadeCore] variant for Compose. Overrides [launch] to dispatch directly
 * to pre-built [ActivityResultLauncher] instances.
 *
 * Every dispatch is guarded by a config null-check — if a media type was
 * not configured, the call is a no-op and [ShadeError.NotConfigured] is
 * surfaced via [onFailure].
 */
internal class ComposeShadeCore(
    private val config: ShadeConfig,
    private val hasPermissionFn: (String) -> Boolean,
    private val cameraPermLauncher: ActivityResultLauncher<String>,
    private val mediaPermLauncher: ActivityResultLauncher<String>,
    private val imageCameraLaunchFn: () -> Unit,
    private val imageGallerySingleLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    private val imageGalleryMultiLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    private val videoCameraLaunchFn: () -> Unit,
    private val videoGallerySingleLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    private val videoGalleryMultiLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    private val pdfLauncher: ActivityResultLauncher<Array<String>>,
    private val documentLauncher: ActivityResultLauncher<Array<String>>,
    private val defaultDocMimes: Array<String>,
    private val getPendingTarget: () -> CameraTarget,
    private val setPendingTarget: (CameraTarget) -> Unit,
) : ShadeCore(registrar = NoOpRegistrar, config = config)
{

    override fun launch(action: ShadeAction) {
        when (action) {
            is ShadeAction.Image.Camera  -> requireCamera(CameraTarget.IMAGE)
            is ShadeAction.Image.Gallery -> launchImageGallery()
            is ShadeAction.Video.Camera  -> requireCamera(CameraTarget.VIDEO)
            is ShadeAction.Video.Gallery -> launchVideoGallery()
            is ShadeAction.Pdf           -> launchPdf()
            is ShadeAction.Document      -> launchDocument(action)
        }
    }

    private fun requireCamera(target: CameraTarget) {
        val configured = when (target) {
            CameraTarget.IMAGE -> config.image?.camera
            CameraTarget.VIDEO -> config.video?.camera
        }
        if (configured == null) {
            configured?.onFailure?.invoke(ShadeError.NotConfigured); return
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
        if (g.isMultiSelect)
            imageGalleryMultiLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        else
            imageGallerySingleLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
    }

    private fun launchVideoGallery() {
        if (config.video?.gallery == null) {
            config.video?.gallery?.onFailure?.invoke(ShadeError.NotConfigured); return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perm = readVideoPermission()
            if (!hasPermissionFn(perm)) { mediaPermLauncher.launch(perm); return }
        }
        launchVideoGalleryInternal(config, videoGallerySingleLauncher, videoGalleryMultiLauncher)
    }

    private fun launchPdf() {
        if (config.pdf == null) {
            config.pdf?.onFailure?.invoke(ShadeError.NotConfigured); return
        }
        pdfLauncher.launch(arrayOf("application/pdf"))
    }

    private fun launchDocument(action: ShadeAction.Document) {
        if (config.document == null) {
            config.document?.onFailure?.invoke(ShadeError.NotConfigured); return
        }
        val mimes = action.mimeTypes.takeIf { it.isNotEmpty() }?.toTypedArray() ?: defaultDocMimes
        documentLauncher.launch(mimes)
    }
}