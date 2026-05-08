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
import com.unitx.shade_core.compose.state.ShadeResultHolder

@Composable
internal fun rememberPermissionLauncher(
    enabled: Boolean,
    onResult: ((Boolean) -> Unit)?
): ActivityResultLauncher<String>? {

    if (!enabled) return null

    return rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onResult?.invoke(granted)
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
    copyToCache: Boolean,
    prefix: String,
    extension: String,
    context: Context,
    callback: ShadeResultHolder
): ActivityResultLauncher<PickVisualMediaRequest>? {

    if (!enabled) return null

    return rememberLauncherForActivityResult(
        PickVisualMedia()
    ) { uri ->

        val result = uri?.let {
            val file = if (copyToCache) {
                FileHelper.copyUriToCache(
                    context,
                    it,
                    prefix,
                    extension
                )
            } else null

            ShadeResult.Single(it, file)
        }

        callback.invoke(result, ShadeError.PickCancelled)
    }
}

@Composable
internal fun rememberMultiMediaLauncher(
    enabled: Boolean,
    maxItems: Int,
    copyToCache: Boolean,
    prefix: String,
    extension: String,
    context: Context,
    callback: ShadeResultHolder
): ActivityResultLauncher<PickVisualMediaRequest>? {

    if (!enabled) return null

    return rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = maxItems.coerceAtLeast(2)
        )
    ) { uris ->

        val items = uris.map { uri ->
            val file = if (copyToCache) {
                FileHelper.copyUriToCache(
                    context,
                    uri,
                    prefix,
                    extension
                )
            } else null

            ShadeResult.ShadeMedia(uri, file)
        }

        val result =
            if (items.isNotEmpty()) ShadeResult.Multiple(items)
            else null

        callback.invoke(result, ShadeError.PickCancelled)
    }
}

@Composable
internal fun rememberDocumentLauncher(
    enabled: Boolean,
    copyToCache: Boolean,
    prefix: String,
    extensionProvider: (Uri) -> String,
    context: Context,
    callback: ShadeResultHolder
): ActivityResultLauncher<Array<String>>? {

    if (!enabled) return null

    return rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->

        val result = uri?.let {
            val file = if (copyToCache) {
                FileHelper.copyUriToCache(
                    context,
                    it,
                    prefix,
                    extensionProvider(it)
                )
            } else null

            if (copyToCache && file == null) null
            else ShadeResult.Single(it, file)
        }

        val error =
            if (uri != null && result == null)
                ShadeError.FileSaveFailed
            else
                ShadeError.PickCancelled

        callback.invoke(result, error)
    }
}