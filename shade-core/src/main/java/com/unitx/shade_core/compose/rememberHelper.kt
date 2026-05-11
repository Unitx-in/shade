package com.unitx.shade_core.compose

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.Composable
import com.unitx.shade_core.common.FileHelper
import com.unitx.shade_core.common.result.ShadeError
import com.unitx.shade_core.common.result.ShadeResult
import com.unitx.shade_core.compose.state.CaptureState
import com.unitx.shade_core.compose.state.PermissionCallbackHolder
import com.unitx.shade_core.compose.state.ShadeResultHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun rememberPermissionLauncher(
    enabled: Boolean,
    permCallbacks: PermissionCallbackHolder,
    which: (PermissionCallbackHolder) -> ((Boolean) -> Unit)?
): ActivityResultLauncher<String>? {
    if (!enabled) return null
    return rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        which(permCallbacks)?.invoke(granted)
    }
}

@Composable
internal fun rememberCaptureLauncher(
    enabled: Boolean,
    contract: ActivityResultContract<Uri, Boolean>,
    captureState: CaptureState,
    callback: ShadeResultHolder
): ActivityResultLauncher<Uri>? {

    if (!enabled) return null

    return rememberLauncherForActivityResult(contract) { success ->
        val file = captureState.file
        val uri = captureState.uri

        captureState.clear()

        val result =
            if (success && file != null && uri != null) {
                ShadeResult.Captured(file, uri)
            } else {
                file?.delete()
                null
            }

        callback.invoke(result, ShadeError.CaptureFailed)
    }
}

@Composable
internal fun rememberSingleMediaLauncher(
    enabled: Boolean,
    callback: ShadeResultHolder
): ActivityResultLauncher<PickVisualMediaRequest>? {

    if (!enabled) return null

    return rememberLauncherForActivityResult(
        PickVisualMedia()
    ) { uri ->

        val result =
            uri?.let {
                ShadeResult.Single(
                    uri = it,
                    file = null
                )
            }

        callback.invoke(result, ShadeError.PickCancelled)
    }
}

@Composable
internal fun rememberMultiMediaLauncher(
    enabled: Boolean,
    maxItems: Int,
    callback: ShadeResultHolder
): ActivityResultLauncher<PickVisualMediaRequest>? {

    if (!enabled) return null

    return rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = maxItems.coerceAtLeast(2)
        )
    ) { uris ->

        val result =
            if (uris.isNotEmpty()) {

                ShadeResult.Multiple(
                    uris.map { uri ->
                        ShadeResult.ShadeMedia(
                            uri = uri,
                            file = null
                        )
                    }
                )

            } else null

        callback.invoke(result, ShadeError.PickCancelled)
    }
}

@Composable
internal fun rememberDocumentLauncher(
    enabled: Boolean,
    callback: ShadeResultHolder
): ActivityResultLauncher<Array<String>>? {

    if (!enabled) return null

    return rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->

        val result = uri?.let {
            ShadeResult.Single(
                uri = it,
                file = null
            )
        }

        callback.invoke(result, ShadeError.PickCancelled)
    }
}