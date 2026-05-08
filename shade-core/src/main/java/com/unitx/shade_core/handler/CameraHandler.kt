package com.unitx.shade_core.handler

import android.Manifest
import android.content.Context
import android.net.Uri
import com.unitx.shade_core.common.CameraTarget
import com.unitx.shade_core.common.FileHelper
import com.unitx.shade_core.common.PermissionHelper
import com.unitx.shade_core.common.config.ShadeConfig
import com.unitx.shade_core.core.LauncherRegistry
import com.unitx.shade_core.common.result.ShadeError
import com.unitx.shade_core.common.result.ShadeResult
import java.io.File

/**
 * Handles all camera-related media flows — image capture and video recording.
 *
 * Responsibilities:
 * - Camera permission request and result routing
 * - Temp file creation and cleanup between launch and result
 * - Dispatching [ShadeResult.Captured] or [ShadeError] to the DSL config callbacks
 */
internal class CameraHandler(
    private val context: Context,
    private val config: ShadeConfig,
    private val registry: LauncherRegistry
) {

    private var pendingTarget: CameraTarget = CameraTarget.IMAGE
    private var tempCaptureFile: File? = null
    private var tempCaptureUri: Uri? = null

    init {
        registry.onCameraPermissionResult = { granted ->
            if (granted) {
                executeCamera(pendingTarget)
            } else {
                val error = if (registry.cameraPermissionLauncher.let {
                        PermissionHelper.shouldShowRationale(context, Manifest.permission.CAMERA)
                    })
                    ShadeError.PermissionDenied
                else
                    ShadeError.PermissionPermanentlyDenied

                when (pendingTarget) {
                    CameraTarget.IMAGE -> config.image?.camera?.onFailure?.invoke(error)
                    CameraTarget.VIDEO -> config.video?.camera?.onFailure?.invoke(error)
                }
            }
            pendingTarget = CameraTarget.IMAGE
        }

        registry.onImageCameraResult = { success ->
            val file = tempCaptureFile
            val uri  = tempCaptureUri
            clearTempState()
            if (success && file != null && uri != null) {
                config.image?.camera?.onResult?.invoke(ShadeResult.Captured(file, uri))
            } else {
                file?.delete()
                config.image?.camera?.onFailure?.invoke(ShadeError.CaptureFailed)
            }
        }

        registry.onVideoCameraResult = { success ->
            val file = tempCaptureFile
            val uri  = tempCaptureUri
            clearTempState()
            if (success && file != null && uri != null) {
                config.video?.camera?.onResult?.invoke(ShadeResult.Captured(file, uri))
            } else {
                file?.delete()
                config.video?.camera?.onFailure?.invoke(ShadeError.CaptureFailed)
            }
        }
    }

    fun handleImageCamera() {
        config.image?.camera ?: return
        requireCameraPermission(CameraTarget.IMAGE)
    }

    fun handleVideoCamera() {
        config.video?.camera ?: return
        requireCameraPermission(CameraTarget.VIDEO)
    }

    private fun requireCameraPermission(target: CameraTarget) {
        pendingTarget = target
        if (PermissionHelper.hasPermission(context, Manifest.permission.CAMERA)) {
            executeCamera(target)
        } else {
            registry.cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun executeCamera(target: CameraTarget) {
        when (target) {
            CameraTarget.IMAGE -> launchImageCamera()
            CameraTarget.VIDEO -> launchVideoCamera()
        }
    }

    private fun launchImageCamera() {
        val (file, uri) = FileHelper.createTempFile(context, "IMG_", ".jpg") ?: run {
            config.image?.camera?.onFailure?.invoke(ShadeError.FileCreationFailed)
            return
        }
        tempCaptureFile = file
        tempCaptureUri  = uri
        registry.imageCameraLauncher.launch(uri)
    }

    private fun launchVideoCamera() {
        val (file, uri) = FileHelper.createTempFile(context, "VID_", ".mp4") ?: run {
            config.video?.camera?.onFailure?.invoke(ShadeError.FileCreationFailed)
            return
        }
        tempCaptureFile = file
        tempCaptureUri  = uri
        registry.videoCameraLauncher.launch(uri)
    }

    private fun clearTempState() {
        tempCaptureFile = null
        tempCaptureUri  = null
    }
}