package com.unitx.shade_core.handler

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
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
import com.unitx.shade_core.common.processor.ImageProcessor
import com.unitx.shade_core.common.processor.VideoProcessor
import com.unitx.shade_core.common.config.extend.CompressionConfig
import com.unitx.shade_core.common.result.ShadeCompressionException
import com.unitx.shade_core.common.result.ShadeFileSaveException
import com.unitx.shade_core.persistence.CameraStatePersistence
import kotlin.coroutines.cancellation.CancellationException

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
    private val scope: CoroutineScope,
    private val cameraStatePersistence: CameraStatePersistence,
) {
    private var pendingTarget: CameraTarget = CameraTarget.IMAGE

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
                        copyToCache = null, // Caching is not available in camera
                        compression = compression,
                        authority = config.getFilesProviderAuthority(),
                        saveToExternalStorage = config.image?.camera?.saveToExternalStorage
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
                        compression = compression,
                        copyToCache = null, // Caching is not available in camera
                        authority = config.getFilesProviderAuthority(),
                        saveToExternalStorage = config.image?.camera?.saveToExternalStorage
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
        cameraStatePersistence.restore()
        val file = cameraStatePersistence.file
        val uri = cameraStatePersistence.uri
        cameraStatePersistence.clear()

        if (!success) {
            file?.delete()
            onFailure?.invoke(ShadeError.CaptureFailed(ShadeError.CaptureFailureReason.ActivityResultFailed))
            return
        }
        if (file == null) {
            onFailure?.invoke(ShadeError.CaptureFailed(ShadeError.CaptureFailureReason.TempFileNull))
            return
        }
        if (uri == null) {
            file.delete()
            onFailure?.invoke(ShadeError.CaptureFailed(ShadeError.CaptureFailureReason.TempUriNull))
            return
        }

        scope.launch {
            try {
                val media = processor(uri, file, prefix, extension, compression)

                if (media.file != null && media.file != file) {
                    file.delete()
                }

                onResult?.invoke(
                    ShadeResult.Captured(
                        file = media.file ?: file,
                        uri = media.uri
                    )
                )
            } catch (e: CancellationException) {
                file.delete()
                Log.i("ShadeError", "Cancellation exception during capture")
                throw e
            } catch (e: ShadeFileSaveException) {
                file.delete()
                onFailure?.invoke(ShadeError.FileSaveFailed(uri = e.uri))
            } catch (e: ShadeCompressionException) {
                file.delete()
                val source = when (prefix) {
                    "VID_" -> ShadeError.CompressionSource.Video
                    else -> ShadeError.CompressionSource.Image
                }
                onFailure?.invoke(ShadeError.CompressionFailed(source = source, cause = e.cause))
            } catch (e: Exception) {
                file.delete()
                onFailure?.invoke(
                    ShadeError.CaptureFailed(
                        reason = ShadeError.CaptureFailureReason.ProcessorFailed,
                        cause = e
                    )
                )
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
        launchCamera(
            prefix = "IMG_",
            extension = ".jpg",
            onFailure = { cause ->
                config.image?.camera?.onFailure?.invoke(ShadeError.FileCreationFailed(cause))
            },
            launcher = registry.imageCameraLauncher
        )
    }

    private fun launchVideoCamera() {
        launchCamera(
            prefix = "VID_",
            extension = ".mp4",
            onFailure = { cause ->
                config.video?.camera?.onFailure?.invoke(ShadeError.FileCreationFailed(cause))
            },
            launcher = registry.videoCameraLauncher
        )
    }

    private fun launchCamera(
        prefix: String,
        extension: String,
        onFailure: ((Throwable?) -> Unit)?,
        launcher: ActivityResultLauncher<Uri>
    ) {
        scope.launch {
            val result = runCatching {
                FileHelper.createTempFile(context, prefix, extension, config.getFilesProviderAuthority())
            }
            val pair = result.getOrNull()
            if (pair == null) {
                onFailure?.invoke(result.exceptionOrNull())
                return@launch
            }
            val (file, uri) = pair
            cameraStatePersistence.save(file, uri)
            launcher.launch(uri)
        }
    }
}