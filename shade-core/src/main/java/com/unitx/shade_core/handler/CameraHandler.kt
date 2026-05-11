package com.unitx.shade_core.handler

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import com.unitx.shade_core.common.CameraTarget
import com.unitx.shade_core.common.FileHelper
import com.unitx.shade_core.common.PermissionHelper
import com.unitx.shade_core.common.config.ShadeConfig
import com.unitx.shade_core.core.LauncherRegistry
import com.unitx.shade_core.common.result.ShadeError
import com.unitx.shade_core.common.result.ShadeResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import com.unitx.shade_core.common.compressor.ImageProcessor
import com.unitx.shade_core.common.compressor.VideoProcessor
import com.unitx.shade_core.common.config.CompressionConfig

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
    private val registry: LauncherRegistry,
    private val scope: CoroutineScope
) {

    private var pendingTarget: CameraTarget = CameraTarget.IMAGE
    private var tempCaptureFile: File? = null
    private var tempCaptureUri: Uri? = null

    init {
        registry.onCameraPermissionResult = result@{ granted ->
            if (!granted) {
                val error = if (PermissionHelper.shouldShowRationale(
                        context,
                        Manifest.permission.CAMERA
                    )
                ) ShadeError.PermissionDenied
                else ShadeError.PermissionPermanentlyDenied

                when (pendingTarget) {
                    CameraTarget.IMAGE -> config.image?.camera?.onFailure?.invoke(error)
                    CameraTarget.VIDEO -> config.video?.camera?.onFailure?.invoke(error)
                }

                pendingTarget = CameraTarget.IMAGE
                return@result
            }



            executeCamera(pendingTarget)
            pendingTarget = CameraTarget.IMAGE
        }

        registry.onImageCameraResult = { success ->

            handleCaptureResult(
                success = success,
                prefix = "IMG_",
                extension = ".jpg",
                compression = config.image?.camera?.compress,
                processor = { uri, file, prefix, extension, compression ->

                    ImageProcessor.process(
                        context = context,
                        uri = uri,
                        file = file,
                        prefix = prefix,
                        extension = extension,
                        compression = compression
                    )
                },
                onFailure = config.image?.camera?.onFailure,
                onResult = config.image?.camera?.onResult
            )
        }

        registry.onVideoCameraResult = { success ->

            handleCaptureResult(
                success = success,
                prefix = "VID_",
                extension = ".mp4",
                compression = config.video?.camera?.compress,
                processor = { uri, file, prefix, extension, compression ->

                    VideoProcessor.process(
                        context = context,
                        uri = uri,
                        file = file,
                        prefix = prefix,
                        extension = extension,
                        compression = compression
                    )
                },
                onFailure = config.video?.camera?.onFailure,
                onResult = config.video?.camera?.onResult
            )
        }
    }

    private fun handleCaptureResult(
        success: Boolean,
        prefix: String,
        extension: String,
        compression: CompressionConfig?,
        processor: suspend (
            uri: Uri,
            file: File?,
            prefix: String,
            extension: String,
            compression: CompressionConfig?
        ) -> ShadeResult.ShadeMedia,
        onFailure: ((ShadeError) -> Unit)?,
        onResult: ((ShadeResult.Captured) -> Unit)?
    ) {

        val file = tempCaptureFile
        val uri = tempCaptureUri

        clearTempState()

        if (!success || file == null || uri == null) {
            file?.delete()
            onFailure?.invoke(ShadeError.CaptureFailed)
            return
        }

        scope.launch {

            val media = processor(
                uri,
                file,
                prefix,
                extension,
                compression
            )

            if (media.file != null && media.file != file) {
                file.delete()
            }

            onResult?.invoke(
                ShadeResult.Captured(
                    file = media.file ?: file,
                    uri = media.uri
                )
            )
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

    private fun launchCamera(
        prefix: String,
        extension: String,
        onFailure: (() -> Unit)?,
        launcher: ActivityResultLauncher<Uri>
    ) {
        val (file, uri) = FileHelper.createTempFile(
            context,
            prefix,
            extension
        ) ?: run {
            onFailure?.invoke()
            return
        }

        tempCaptureFile = file
        tempCaptureUri = uri

        launcher.launch(uri)
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
        launchCamera(
            prefix = "IMG_",
            extension = ".jpg",
            onFailure = {
                config.image?.camera?.onFailure?.invoke(
                    ShadeError.FileCreationFailed
                )
            },
            launcher = registry.imageCameraLauncher
        )
    }

    private fun launchVideoCamera() {
        launchCamera(
            prefix = "VID_",
            extension = ".mp4",
            onFailure = {
                config.video?.camera?.onFailure?.invoke(
                    ShadeError.FileCreationFailed
                )
            },
            launcher = registry.videoCameraLauncher
        )
    }

    private fun clearTempState() {
        tempCaptureFile = null
        tempCaptureUri = null
    }
}