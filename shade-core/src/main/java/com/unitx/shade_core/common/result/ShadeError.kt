package com.unitx.shade_core.common.result

/**
 * Every failure surface in Shade maps to one of these variants.
 * Received in `onFailure { error -> }` blocks inside your DSL config.
 *
 * ```kotlin
 * onFailure { error ->
 *     when (error) {
 *         ShadeError.PermissionDenied           -> showRationale()
 *         ShadeError.PermissionPermanentlyDenied -> openAppSettings()
 *         ShadeError.PickCancelled               -> Unit
 *         is ShadeError.CaptureFailed            -> log(error.reason)
 *         is ShadeError.CompressionFailed        -> log(error.cause)
 *         is ShadeError.Unknown                  -> log(error.throwable)
 *         else                                   -> showGenericError()
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

    // ─── Capture ─────────────────────────────────────────────────────────────

    /**
     * Camera or video capture returned failure.
     * [reason] tells you exactly why:
     * - [CaptureFailureReason.ActivityResultFailed] — system returned RESULT_CANCELED
     * - [CaptureFailureReason.TempFileNull] — temp file was null before capture started
     * - [CaptureFailureReason.TempUriNull] — temp URI was null before capture started
     * - [CaptureFailureReason.ProcessorFailed] — processor threw after successful capture
     */
    data class CaptureFailed(
        val reason: CaptureFailureReason = CaptureFailureReason.ActivityResultFailed,
        val cause: Throwable? = null
    ) : ShadeError()

    enum class CaptureFailureReason {
        ActivityResultFailed,  // success = false from ActivityResult
        TempFileNull,          // tempCaptureFile was null when result arrived
        TempUriNull,           // tempCaptureUri was null when result arrived
        ProcessorFailed        // processor threw during image/video processing
    }

    /** The picker was dismissed without a selection. */
    data object PickCancelled : ShadeError()

    /** The picker returned a result but URI was null or unusable. */
    data object PickFailed : ShadeError()

    // ─── File / IO ────────────────────────────────────────────────────────────

    /**
     * Temp file could not be created before camera launch.
     * [cause] holds the original IOException if available.
     */
    data class FileCreationFailed(val cause: Throwable? = null) : ShadeError()

    /**
     * Content could not be copied to cache.
     * For single-select: [uri] is the URI that failed.
     * For multi-select: [uris] lists all URIs that failed.
     * [cause] holds the original exception.
     */
    data class FileSaveFailed(
        val uri: android.net.Uri? = null,
        val uris: List<android.net.Uri> = emptyList(),
        val cause: Throwable? = null
    ) : ShadeError() {

        /** All failed URIs regardless of whether single or multi. */
        val allFailed: List<android.net.Uri>
            get() = if (uri != null) listOf(uri) else uris
    }

    /**
     * An existing file or URI could not be read.
     * [uri] is the URI that failed. [cause] holds the original exception.
     */
    data class FileReadFailed(
        val uri: android.net.Uri? = null,
        val cause: Throwable? = null
    ) : ShadeError()

    // ─── Compression ─────────────────────────────────────────────────────────

    /**
     * Compression was enabled but failed.
     * For single: [cause] holds the exception.
     * For multi: [failedUris] lists which URIs failed.
     * [source] is which compressor failed — image or video.
     */
    data class CompressionFailed(
        val source: CompressionSource = CompressionSource.Image,
        val cause: Throwable? = null,
        val failedUris: List<android.net.Uri> = emptyList()
    ) : ShadeError() {

        /** True if this was a partial failure in a multi-select batch. */
        val isPartialFailure: Boolean
            get() = failedUris.isNotEmpty()
    }

    enum class CompressionSource { Image, Video }

    // ─── Document ────────────────────────────────────────────────────────────

    /**
     * One or more documents in a multi-select failed to process.
     * [failedUris] lists which URIs failed.
     */
    data class DocumentProcessingFailed(
        val failedUris: List<android.net.Uri> = emptyList(),
        val cause: Throwable? = null
    ) : ShadeError()

    // ─── Misuse ───────────────────────────────────────────────────────────────

    /** launch() was called for a media type with no matching config block. */
    data object NotConfigured : ShadeError()

    /** An action was dispatched that Shade does not recognise. */
    data object UnsupportedAction : ShadeError()

    // ─── Unknown ──────────────────────────────────────────────────────────────

    /** Catch-all for unexpected exceptions. */
    data class Unknown(val throwable: Throwable? = null) : ShadeError()
}