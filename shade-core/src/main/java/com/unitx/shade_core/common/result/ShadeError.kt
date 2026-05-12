package com.unitx.shade_core.common.result

/**
 * Every failure surface in Shade maps to one of these variants.
 * Received in `onFailure { error -> }` blocks inside your DSL config.
 *
 * ```kotlin
 * onFailure { error ->
 *     when (error) {
 *         ShadeError.PermissionDenied -> showRationale()
 *         ShadeError.PermissionPermanentlyDenied -> openAppSettings()
 *         ShadeError.PickCancelled -> Unit // user backed out, no action needed
 *         else -> showGenericError()
 *     }
 * }
 * ```
 */
sealed class ShadeError {

    // ─── Permissions ─────────────────────────────────────────────────────────

    /** Permission denied — can be re-requested. */
    data object PermissionDenied : ShadeError()

    /** Permission denied with "Don't ask again" — redirect user to app settings. */
    data object PermissionPermanentlyDenied : ShadeError()

    // ─── Capture / Picker ────────────────────────────────────────────────────

    /** Camera or video capture was launched but the system returned failure. */
    data object CaptureFailed : ShadeError()

    /** The picker was dismissed without a selection. */
    data object PickCancelled : ShadeError()

    /** The picker returned a result but it could not be used (null URI, etc.). */
    data object PickFailed : ShadeError()

    // ─── File / IO ────────────────────────────────────────────────────────────

    /** A temp file could not be created before camera capture. */
    data object FileCreationFailed : ShadeError()

    /** Content could not be copied to cache. Emitted when `copyToCache` is enabled and fails. */
    data object FileSaveFailed : ShadeError()

    /** An existing file or URI could not be read. */
    data object FileReadFailed : ShadeError()

    // ─── Compression ─────────────────────────────────────────────────────────

    /** Compression was enabled but failed. The original file is not delivered. */
    data object CompressionFailed : ShadeError()

    // ─── Misuse ───────────────────────────────────────────────────────────────

    /** [ShadeCore.launch] was called for a media type with no matching config block. */
    data object NotConfigured : ShadeError()

    /** An action was dispatched that Shade does not recognise. */
    data object UnsupportedAction : ShadeError()

    // ─── Unknown ──────────────────────────────────────────────────────────────

    /** Catch-all for unexpected exceptions. */
    data class Unknown(val throwable: Throwable? = null) : ShadeError()
}