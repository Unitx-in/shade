package com.unitx.shade_core.common.result

import android.net.Uri
import java.io.File

/**
 * Unified result type returned by all Shade operations.
 *
 * ```kotlin
 * onResult { result ->
 *     when (result) {
 *         is ShadeResult.Captured  -> use(result.file, result.uri)
 *         is ShadeResult.Single    -> use(result.uri, result.file)
 *         is ShadeResult.Multiple  -> result.items.forEach { use(it.uri, it.file) }
 *     }
 * }
 * ```
 */
sealed class ShadeResult {

    /**
     * A single item from a gallery or document picker.
     *
     * [file] is non-null only when `copyToCache` or `compress` was enabled.
     * Without either, only [uri] is available.
     */
    data class Single(
        val uri: Uri,
        val file: File? = null
    ) : ShadeResult()

    /**
     * Multiple items from a multi-select gallery or document pick.
     *
     * Each [ShadeMedia] item follows the same [Single] contract —
     * [ShadeMedia.file] is non-null only when `copyToCache` or `compress` was enabled.
     */
    data class Multiple(
        val items: List<ShadeMedia>
    ) : ShadeResult()

    /**
     * A single media or document item within a [Multiple] result.
     *
     * [file] is non-null only when `copyToCache` or `compress` was enabled.
     */
    data class ShadeMedia(
        val uri: Uri,
        val file: File? = null
    )

    /**
     * An item captured by the device camera (image or video).
     *
     * Both [file] and [uri] are always non-null for camera results.
     */
    data class Captured(
        val file: File,
        val uri: Uri
    ) : ShadeResult()
}