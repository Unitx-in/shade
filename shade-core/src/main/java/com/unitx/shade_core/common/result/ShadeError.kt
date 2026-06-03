package com.unitx.shade_core.common.result

/**
 * Every failure surface in Shade maps to one of these variants.
 * Received in `onFailure { error -> }` blocks inside your DSL config.
 *
 * ## Usage
 * ```kotlin
 * onFailure { error ->
 *     when (error) {
 *         ShadeError.PermissionDenied            -> showRationale()
 *         ShadeError.PermissionPermanentlyDenied -> openAppSettings()
 *         ShadeError.PickCancelled               -> Unit // user backed out, no action needed
 *         is ShadeError.CaptureFailed            -> handleCapture(error.reason, error.cause)
 *         is ShadeError.FileCreationFailed       -> handleStorageError(error.cause)
 *         is ShadeError.FileSaveFailed           -> retryOrSkip(error.allFailed)
 *         is ShadeError.CompressionFailed        -> {
 *             if (error.isPartialFailure) retryFailed(error.failedUris)
 *             else showError(error.cause)
 *         }
 *         is ShadeError.DocumentProcessingFailed -> showFailedDocs(error.failedUris)
 *         is ShadeError.Unknown                  -> log(error.throwable)
 *         else                                   -> showGenericError()
 *     }
 * }
 * ```
 *
 * ## Error origin map
 *
 * | Error | When it fires |
 * |---|---|
 * | [PermissionDenied] | Camera or media permission denied, can re-request |
 * | [PermissionPermanentlyDenied] | Permission denied with "Don't ask again" |
 * | [PickCancelled] | User dismissed the picker or back-pressed without selecting |
 * | [PickFailed] | Picker returned a result but URI was null or unusable |
 * | [CaptureFailed] | Camera capture failed — see [CaptureFailureReason] for exact cause |
 * | [FileCreationFailed] | Temp file could not be created before camera launch |
 * | [FileSaveFailed] | copyToCache was enabled but file could not be written |
 * | [FileReadFailed] | An existing file or URI could not be opened for reading |
 * | [CompressionFailed] | Compression was enabled but failed — partial or total |
 * | [DocumentProcessingFailed] | One or more documents failed during processing |
 * | [NotConfigured] | launch() called for a type with no matching config block |
 * | [UnsupportedAction] | An action was dispatched that Shade does not recognise |
 * | [Unknown] | Unexpected exception — [Unknown.throwable] has the full stack |
 */
sealed class ShadeError {

    // ─── Permissions ─────────────────────────────────────────────────────────

    /**
     * Camera or media permission was denied but can be re-requested.
     * Show a rationale dialog and call [ShadeCore.launch] again.
     */
    data object PermissionDenied : ShadeError()

    /**
     * Camera or media permission was permanently denied ("Don't ask again").
     * Redirect the user to app settings — the system will no longer show the prompt.
     */
    data object PermissionPermanentlyDenied : ShadeError()

    // ─── Capture ─────────────────────────────────────────────────────────────

    /**
     * Camera or video capture did not complete successfully.
     *
     * Check [reason] to understand the exact failure point:
     * - [CaptureFailureReason.ActivityResultFailed] — the system returned `RESULT_CANCELED`;
     *   the user may have backed out, or the camera app crashed.
     * - [CaptureFailureReason.TempFileNull] — the temp file Shade created before launching
     *   the camera was null when the result arrived; likely a storage issue.
     * - [CaptureFailureReason.TempUriNull] — same as above but for the URI.
     * - [CaptureFailureReason.ProcessorFailed] — capture succeeded but
     *   [ImageProcessor] or [VideoProcessor] threw during post-processing;
     *   [cause] holds the original exception.
     */
    data class CaptureFailed(
        val reason: CaptureFailureReason = CaptureFailureReason.ActivityResultFailed,
        val cause: Throwable? = null
    ) : ShadeError()

    /**
     * The picker was dismissed without a selection (user pressed back).
     * No action is usually needed — treat as a no-op.
     */
    data object PickCancelled : ShadeError()

    /**
     * The picker returned a result but the URI was null or could not be used.
     * May indicate a system-level issue with the media picker.
     */
    data object PickFailed : ShadeError()

    // ─── File / IO ────────────────────────────────────────────────────────────

    /**
     * Shade could not create the temporary file needed before launching the camera.
     * Usually caused by insufficient storage or a permissions issue on the cache directory.
     * [cause] holds the original [IOException] if available.
     */
    data class FileCreationFailed(val cause: Throwable? = null) : ShadeError()

    /**
     * Content could not be copied to cache. Only fired when `copyToCache` is enabled.
     *
     * For single-select: [uri] is the URI that failed.
     * For multi-select: [uris] lists every URI that failed.
     * Use [allFailed] to get all failed URIs without branching.
     * [cause] holds the original exception if available.
     */
    data class FileSaveFailed(
        val uri: android.net.Uri? = null,
        val uris: List<android.net.Uri> = emptyList(),
        val cause: Throwable? = null
    ) : ShadeError() {

        /** All failed URIs regardless of whether this came from a single or multi pick. */
        val allFailed: List<android.net.Uri>
            get() = if (uri != null) listOf(uri) else uris
    }

    /**
     * An existing file or URI could not be opened for reading.
     * [uri] is the URI that caused the failure. [cause] holds the original exception.
     */
    data class FileReadFailed(
        val uri: android.net.Uri? = null,
        val cause: Throwable? = null
    ) : ShadeError()

    // ─── Compression ─────────────────────────────────────────────────────────

    /**
     * Compression was enabled but failed. Only fired when `compress` is enabled.
     *
     * [source] identifies which compressor failed — [CompressionSource.Image] or [CompressionSource.Video].
     * [cause] holds the original exception for single-file failures.
     * [failedUris] lists the specific URIs that failed in a multi-select batch.
     * Use [isPartialFailure] to check whether some items succeeded and others did not.
     *
     * The original uncompressed file is not delivered when this error fires.
     */
    data class CompressionFailed(
        val source: CompressionSource = CompressionSource.Image,
        val cause: Throwable? = null,
        val failedUris: List<android.net.Uri> = emptyList()
    ) : ShadeError() {

        /**
         * `true` if at least one item in a multi-select batch failed compression
         * while others may have succeeded. `false` for a total single-file failure.
         */
        val isPartialFailure: Boolean
            get() = failedUris.isNotEmpty()
    }

    // ─── Document ────────────────────────────────────────────────────────────

    /**
     * One or more documents could not be processed after being picked.
     * [failedUris] lists every URI that failed so the caller can retry selectively.
     * [cause] holds the original exception if available.
     */
    data class DocumentProcessingFailed(
        val failedUris: List<android.net.Uri> = emptyList(),
        val cause: Throwable? = null
    ) : ShadeError()

    // ─── Misuse ───────────────────────────────────────────────────────────────

    /**
     * [ShadeCore.launch] was called for a media type that has no matching config block.
     * For example, calling `launch(ShadeAction.Image.Camera)` without configuring
     * `config.image { camera { ... } }`.
     */
    data object NotConfigured : ShadeError()

    /**
     * An action was dispatched that Shade does not recognise.
     * Should not occur in normal use — indicates a library version mismatch
     * or an incorrect [ShadeAction] subclass.
     */
    data object UnsupportedAction : ShadeError()

    // ─── Unknown ──────────────────────────────────────────────────────────────

    /**
     * An unexpected exception occurred that does not map to any known error variant.
     * [throwable] holds the full exception for logging or crash reporting.
     * If you see this frequently, please file a bug with the stack trace.
     */
    data class Unknown(val throwable: Throwable? = null) : ShadeError()

    // ─── Supporting enums ─────────────────────────────────────────────────────

    /**
     * Describes exactly where a camera capture operation failed.
     * Returned as part of [CaptureFailed.reason].
     */
    enum class CaptureFailureReason {

        /** The `ActivityResult` returned `success = false`. The user may have cancelled
         *  or the camera app returned an error result. */
        ActivityResultFailed,

        /** The temporary capture file was `null` when the result arrived.
         *  Usually indicates a storage or cache directory issue. */
        TempFileNull,

        /** The temporary capture URI was `null` when the result arrived.
         *  Usually indicates a [FileProvider] configuration issue. */
        TempUriNull,

        /** Capture succeeded but post-processing (compression or file move) threw an exception.
         *  Check [CaptureFailed.cause] for the original exception. */
        ProcessorFailed
    }

    /** Identifies which media type triggered a [CompressionFailed] error. */
    enum class CompressionSource {
        /** Failure originated in [ImageCompressor]. */
        Image,
        /** Failure originated in [VideoCompressor]. */
        Video
    }
}