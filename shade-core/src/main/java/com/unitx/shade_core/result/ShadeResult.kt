package com.unitx.shade_core.result

import android.net.Uri
import java.io.File

/**
 * Unified result type returned by all Shade media operations.
 *
 * Every action yields exactly one of these variants so callers never
 * have to deal with mixed (File, Uri) / Uri-only callback signatures.
 *
 * Usage in DSL:
 * ```
 * image {
 *     camera {
 *         onResult { result ->
 *             val file = (result as ShadeResult.Captured).file
 *             val uri  = result.uri
 *         }
 *     }
 *     gallery {
 *         multiSelect(maxItems = 5)
 *         onResult { result ->
 *             when (result) {
 *                 is ShadeResult.Single   -> load(result.uri)
 *                 is ShadeResult.Multiple -> result.uris.forEach { load(it) }
 *                 else -> Unit
 *             }
 *         }
 *     }
 * }
 * ```
 */
sealed class ShadeResult {

    /**
     * A single media item chosen from a gallery / document picker.
     * [file] is non-null only when Shade had to copy the content to a
     * cache file (e.g. PDF / document — so the caller has a stable path).
     */
    data class Single(
        val uri: Uri,
        val file: File? = null
    ) : ShadeResult()

    /**
     * Multiple media items returned from a multi-select gallery pick.
     */
    data class Multiple(
        val uris: List<Uri>
    ) : ShadeResult()

    /**
     * An item that was captured by the device camera (image or video).
     * Both [file] and [uri] are always non-null.
     */
    data class Captured(
        val file: File,
        val uri: Uri
    ) : ShadeResult()
}