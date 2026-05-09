package com.unitx.shade_core.common.result

/**
 * Every failure surface in Shade maps to one of these variants.
 * Pass these into [onFailure] blocks inside your DSL config.
 */
sealed class ShadeError {

    // ─── Permissions ─────────────────────────────────────────────────────────

    /** The user denied the requested permission (dismissable — can ask again). */
    data object PermissionDenied : ShadeError()

    /** The user denied the permission and checked "Don't ask again". */
    data object PermissionPermanentlyDenied : ShadeError()

    // ─── Capture / Picker failures ────────────────────────────────────────────

    /** Camera or video capture was launched but the system returned failure. */
    data object CaptureFailed : ShadeError()

    /** The picker was opened but the user cancelled / nothing was returned. */
    object PickCancelled : ShadeError()

    /** The picker returned a result but it could not be used (null URI, etc.). */
    data object PickFailed : ShadeError()

    // ─── File / IO ────────────────────────────────────────────────────────────

    /** A temporary or cache file could not be created before capture. */
    data object FileCreationFailed : ShadeError()

    /** Content could not be copied / saved to the target file. */
    data object FileSaveFailed : ShadeError()

    /** An existing file or URI could not be read. */
    data object FileReadFailed : ShadeError()

    // ─── Unsupported / Misuse ─────────────────────────────────────────────────

    /**
     * [ShadeCore.launch] was called for a media type that has no matching
     * config block in [ShadeConfig].
     */
    data object NotConfigured : ShadeError()

    /** An action was dispatched that Shade does not recognise. */
    data object UnsupportedAction : ShadeError()
    data object CompressionFailed : ShadeError()

    // ─── Unknown ──────────────────────────────────────────────────────────────

    /** Catch-all for unexpected exceptions. */
    data class Unknown(val throwable: Throwable? = null) : ShadeError()
}