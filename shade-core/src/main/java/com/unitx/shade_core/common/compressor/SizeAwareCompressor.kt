package com.unitx.shade_core.common.compressor

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Compresses a [Bitmap] to a [File] respecting a maximum file size.
 *
 * Strategy:
 * 1. Binary search on quality from [startQuality] down to [minQuality].
 * 2. If minQuality still exceeds limit, scale resolution by 0.75x and retry.
 * 3. Stops when file fits or shortest side drops below [MIN_SIDE_PX].
 */
internal object SizeAwareCompressor {

    private const val SCALE_STEP = 0.75f
    private const val MIN_SIDE_PX = 100

    /**
     * @param bitmap       Source bitmap (already orientation-corrected and resized).
     * @param outFile      Destination file to write compressed bytes into.
     * @param format       Bitmap compress format.
     * @param startQuality Starting quality (from [CompressionConfig.quality]).
     * @param minQuality   Floor quality before resolution scaling kicks in.
     * @param maxFileSizeKb Target max size in kilobytes.
     * @return Final file size in bytes.
     */
    fun compress(
        bitmap: Bitmap,
        outFile: File,
        format: Bitmap.CompressFormat,
        startQuality: Int,
        minQuality: Int,
        maxFileSizeKb: Double
    ): Long {
        val maxBytes = (maxFileSizeKb * 1024).toLong()
        var currentBitmap = bitmap
        var scaleFactor = 1.0f

        while (true) {
            val bestQuality = binarySearchQuality(
                bitmap = currentBitmap,
                format = format,
                startQuality = startQuality,
                minQuality = minQuality,
                maxBytes = maxBytes
            )

            writeToFile(currentBitmap, outFile, format, bestQuality)

            // Fits — done
            if (outFile.length() <= maxBytes) break

            // Doesn't fit even at minQuality — scale resolution down
            scaleFactor *= SCALE_STEP
            val newWidth = (bitmap.width * scaleFactor).toInt()
            val newHeight = (bitmap.height * scaleFactor).toInt()

            // Too small to reduce further — accept best effort
            if (newWidth < MIN_SIDE_PX || newHeight < MIN_SIDE_PX) break

            if (currentBitmap !== bitmap) currentBitmap.recycle()
            currentBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }

        if (currentBitmap !== bitmap) currentBitmap.recycle()
        return outFile.length()
    }

    /**
     * Binary search for the highest quality that produces output ≤ [maxBytes].
     * Returns [minQuality] if nothing fits.
     */
    private fun binarySearchQuality(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        startQuality: Int,
        minQuality: Int,
        maxBytes: Long
    ): Int {
        // Fast path — already fits at desired quality
        if (sizeAtQuality(bitmap, format, startQuality) <= maxBytes) return startQuality

        var lo = minQuality
        var hi = startQuality
        var best = minQuality

        while (lo <= hi) {
            val mid = (lo + hi) / 2
            if (sizeAtQuality(bitmap, format, mid) <= maxBytes) {
                best = mid
                lo = mid + 1  // fits — try higher quality
            } else {
                hi = mid - 1  // too large — go lower
            }
        }
        return best
    }

    private fun sizeAtQuality(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int): Long {
        val bos = ByteArrayOutputStream()
        bitmap.compress(format, quality, bos)
        return bos.size().toLong()
    }

    private fun writeToFile(bitmap: Bitmap, file: File, format: Bitmap.CompressFormat, quality: Int) {
        file.outputStream().use { bitmap.compress(format, quality, it) }
    }
}