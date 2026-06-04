package com.unitx.shade_core.handler

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
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
import kotlinx.coroutines.async
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
    private val scope: CoroutineScope
) {

    private var pendingTarget: CameraTarget = CameraTarget.IMAGE
    private var tempCaptureFile: File? = null
    private var tempCaptureUri: Uri? = null

    private val prefs by lazy {
        context.applicationContext.getSharedPreferences("shade_temp", Context.MODE_PRIVATE)
    }

    init {
        Log.d("Shade", "CameraHandler — init")

        registry.onCameraPermissionResult = result@{ granted ->
            Log.d("Shade", "onCameraPermissionResult — granted=$granted, pendingTarget=$pendingTarget")
            if (!granted) {
                val error = if (PermissionHelper.shouldShowRationale(
                        context,
                        Manifest.permission.CAMERA
                    )
                ) ShadeError.PermissionDenied
                else ShadeError.PermissionPermanentlyDenied

                Log.w("Shade", "onCameraPermissionResult — denied, error=$error")
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
            Log.d("Shade", "onImageCameraResult — success=$success")
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
                        copyToCache = null,
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
            Log.d("Shade", "onVideoCameraResult — success=$success")
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
                        copyToCache = null,
                        authority = config.getFilesProviderAuthority(),
                        saveToExternalStorage = config.video?.camera?.saveToExternalStorage
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
        restoreTempState()
        val file = tempCaptureFile
        val uri = tempCaptureUri
        Log.d("Shade", "handleCaptureResult — success=$success, file=$file, uri=$uri")
        clearTempState()

        if (!success) {
            Log.w("Shade", "handleCaptureResult — camera returned success=false, deleting file=$file")
            file?.delete()
            onFailure?.invoke(ShadeError.CaptureFailed(ShadeError.CaptureFailureReason.ActivityResultFailed))
            return
        }
        if (file == null) {
            Log.e("Shade", "handleCaptureResult — TempFileNull after restore")
            onFailure?.invoke(ShadeError.CaptureFailed(ShadeError.CaptureFailureReason.TempFileNull))
            return
        }
        if (uri == null) {
            Log.e("Shade", "handleCaptureResult — TempUriNull after restore, deleting file=$file")
            file.delete()
            onFailure?.invoke(ShadeError.CaptureFailed(ShadeError.CaptureFailureReason.TempUriNull))
            return
        }

        scope.launch {
            try {
                Log.d("Shade", "handleCaptureResult — starting processor, prefix=$prefix")
                val media = processor(uri, file, prefix, extension, compression)
                Log.d("Shade", "handleCaptureResult — processor done, media.file=${media.file}, media.uri=${media.uri}")

                if (media.file != null && media.file != file) {
                    Log.d("Shade", "handleCaptureResult — deleting original temp file=$file")
                    file.delete()
                }

                onResult?.invoke(
                    ShadeResult.Captured(
                        file = media.file ?: file,
                        uri = media.uri
                    )
                )
                Log.d("Shade", "handleCaptureResult — onResult invoked")

            } catch (e: CancellationException) {
                Log.i("Shade", "handleCaptureResult — CancellationException, deleting file=$file")
                file.delete()
                throw e
            } catch (e: ShadeFileSaveException) {
                Log.e("Shade", "handleCaptureResult — ShadeFileSaveException", e)
                file.delete()
                onFailure?.invoke(ShadeError.FileSaveFailed(uri = e.uri))
            } catch (e: ShadeCompressionException) {
                Log.e("Shade", "handleCaptureResult — ShadeCompressionException", e)
                file.delete()
                val source = when (prefix) {
                    "VID_" -> ShadeError.CompressionSource.Video
                    else -> ShadeError.CompressionSource.Image
                }
                onFailure?.invoke(ShadeError.CompressionFailed(source = source, cause = e.cause))
            } catch (e: Exception) {
                Log.e("Shade", "handleCaptureResult — unexpected Exception", e)
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
        Log.d("Shade", "handleImageCamera — called")
        config.image?.camera ?: return
        requireCameraPermission(CameraTarget.IMAGE)
    }

    fun handleVideoCamera() {
        Log.d("Shade", "handleVideoCamera — called")
        config.video?.camera ?: return
        requireCameraPermission(CameraTarget.VIDEO)
    }

    private fun requireCameraPermission(target: CameraTarget) {
        Log.d("Shade", "requireCameraPermission — target=$target")
        pendingTarget = target
        if (PermissionHelper.hasPermission(context, Manifest.permission.CAMERA)) {
            Log.d("Shade", "requireCameraPermission — permission already granted")
            executeCamera(target)
        } else {
            Log.d("Shade", "requireCameraPermission — requesting permission")
            registry.cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun executeCamera(target: CameraTarget) {
        Log.d("Shade", "executeCamera — target=$target")
        when (target) {
            CameraTarget.IMAGE -> launchImageCamera()
            CameraTarget.VIDEO -> launchVideoCamera()
        }
    }

    private fun launchImageCamera() {
        Log.d("Shade", "launchImageCamera — called")
        launchCamera(
            prefix = "IMG_",
            extension = ".jpg",
            onFailure = { cause ->
                Log.e("Shade", "launchImageCamera — FileCreationFailed", cause)
                config.image?.camera?.onFailure?.invoke(ShadeError.FileCreationFailed(cause))
            },
            launcher = registry.imageCameraLauncher
        )
    }

    private fun launchVideoCamera() {
        Log.d("Shade", "launchVideoCamera — called")
        launchCamera(
            prefix = "VID_",
            extension = ".mp4",
            onFailure = { cause ->
                Log.e("Shade", "launchVideoCamera — FileCreationFailed", cause)
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
            Log.d("Shade", "launchCamera — creating temp file, prefix=$prefix, ext=$extension")
            val result = runCatching {
                FileHelper.createTempFile(context, prefix, extension, config.getFilesProviderAuthority())
            }
            val pair = result.getOrNull()
            if (pair == null) {
                Log.e("Shade", "launchCamera — createTempFile failed", result.exceptionOrNull())
                onFailure?.invoke(result.exceptionOrNull())
                return@launch
            }
            val (file, uri) = pair
            Log.d("Shade", "launchCamera — temp file created: file=${file.absolutePath}, uri=$uri")
            tempCaptureFile = file
            tempCaptureUri = uri
            saveTempState(file, uri)
            launcher.launch(uri)
            Log.d("Shade", "launchCamera — launcher launched")
        }
    }

    private fun saveTempState(file: File, uri: Uri) {
        Log.d("Shade", "saveTempState — file=${file.absolutePath}, uri=$uri")
        prefs.edit()
            .putString("temp_file", file.absolutePath)
            .putString("temp_uri", uri.toString())
            .apply()
    }

    private fun restoreTempState() {
        if (tempCaptureFile != null && tempCaptureUri != null) {
            Log.d("Shade", "restoreTempState — already in memory, skipping")
            return
        }
        val path = prefs.getString("temp_file", null)
        val uriStr = prefs.getString("temp_uri", null)
        Log.d("Shade", "restoreTempState — prefs: file=$path, uri=$uriStr")
        if (path != null && uriStr != null) {
            tempCaptureFile = File(path)
            tempCaptureUri = Uri.parse(uriStr)
            Log.d("Shade", "restoreTempState — restored from prefs")
        } else {
            Log.w("Shade", "restoreTempState — nothing in prefs, tempState will be null")
        }
    }

    private fun clearTempState() {
        Log.d("Shade", "clearTempState — clearing memory and prefs")
        tempCaptureFile = null
        tempCaptureUri = null
        prefs.edit().clear().apply()
    }
}