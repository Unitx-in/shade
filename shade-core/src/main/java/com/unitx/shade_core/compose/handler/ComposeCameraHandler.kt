package com.unitx.shade_core.compose.handler

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import com.unitx.shade_core.common.CameraTarget
import com.unitx.shade_core.common.FileHelper
import com.unitx.shade_core.common.compressor.ImageProcessor
import com.unitx.shade_core.common.PermissionHelper
import com.unitx.shade_core.common.compressor.VideoProcessor
import com.unitx.shade_core.compose.state.CaptureState
import com.unitx.shade_core.compose.state.PermissionCallbackHolder
import com.unitx.shade_core.compose.state.ShadeResultHolder
import com.unitx.shade_core.common.config.ShadeConfig
import com.unitx.shade_core.common.result.ShadeError
import com.unitx.shade_core.common.result.ShadeResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Compose-side camera handler.
 *
 * Wires permission and capture result callbacks, owns [CaptureState],
 * and exposes [handleImageCamera] / [handleVideoCamera] to [ComposeShadeCore].
 */
internal class ComposeCameraHandler(
    private val context: Context,
    private val config: ShadeConfig,
    private val captureState: CaptureState,
    private val cameraPermLauncher: ActivityResultLauncher<String>?,
    private val imageCameraLauncher: ActivityResultLauncher<Uri>?,
    private val videoCameraLauncher: ActivityResultLauncher<Uri>?,
    private val scope: CoroutineScope,
    imageCameraCallback: ShadeResultHolder,
    videoCameraCallback: ShadeResultHolder,
    permCallbacks: PermissionCallbackHolder,
) {

    var pendingTarget: CameraTarget = CameraTarget.IMAGE

    init {
        // ── Image camera result ───────────────────────────────────────────────
        imageCameraCallback.onResult = onResult@{ result ->

            val captured = result as ShadeResult.Captured
            val cameraConfig = config.image?.camera ?: return@onResult

            scope.launch {

                val processed = ImageProcessor.process(
                    context = context,
                    uri = captured.uri,
                    file = captureState.file,
                    prefix = "IMG_",
                    extension = ".jpg",
                    compression = cameraConfig.compress
                )

                cameraConfig.onResult?.invoke(
                    ShadeResult.Captured(
                        file = processed.file!!,
                        uri = processed.uri
                    )
                )
            }
        }

        imageCameraCallback.onFailure = { config.image?.camera?.onFailure?.invoke(it) }

        // ── Video camera result ───────────────────────────────────────────────
        videoCameraCallback.onResult = onResult@{ result ->

            val captured = result as ShadeResult.Captured
            val cameraConfig = config.video?.camera ?: return@onResult

            scope.launch {

                val processed = VideoProcessor.process(
                    context = context,
                    uri = captured.uri,
                    file = captureState.file,
                    prefix = "VID_",
                    extension = ".mp4",
                    compression = cameraConfig.compress
                )

                cameraConfig.onResult?.invoke(
                    ShadeResult.Captured(
                        file = processed.file!!,
                        uri = processed.uri
                    )
                )
            }
        }

        videoCameraCallback.onFailure = { config.video?.camera?.onFailure?.invoke(it) }

        // ── Camera permission result ──────────────────────────────────────────
        permCallbacks.onCamera = { granted ->
            if (granted) {
                executeCamera(pendingTarget)
            } else {
                val error =
                    if (PermissionHelper.shouldShowRationale(context, Manifest.permission.CAMERA))
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
            cameraPermLauncher?.launch(Manifest.permission.CAMERA)
        }
    }

    private fun executeCamera(target: CameraTarget) {
        when (target) {
            CameraTarget.IMAGE -> launchImageCamera()
            CameraTarget.VIDEO -> launchVideoCamera()
        }
    }

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
}