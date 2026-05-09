package com.unitx.shade_core.common.compressor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import com.unitx.shade_core.common.compressor.CompressFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * High-performance image compressor with down-sampling,
 * orientation correction, and configurable output format.
 */
internal object ImageCompressor {

    /**
     * Compresses an image file.
     *
     * @param context       Application context (used for cache directory)
     * @param input         Original image file
     * @param quality       JPEG quality (0–100). Ignored for PNG output.
     * @param maxWidth      Maximum allowed width (preserves aspect ratio). `null` = no limit.
     * @param maxHeight     Maximum allowed height (preserves aspect ratio). `null` = no limit.
     * @param outputFormat  Desired output format (JPEG or PNG).
     *
     * @return Compressed file, or `null` if compression failed.
     */
    suspend fun compress(
        context: Context,
        input: File,
        quality: Int,
        maxWidth: Int?,
        maxHeight: Int?,
        outputFormat: CompressFormat = CompressFormat.JPEG
    ): File? = withContext(Dispatchers.IO) {

        try {

            // ── Decode bounds only ────────────────────────────────────────────

            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            BitmapFactory.decodeFile(
                input.absolutePath,
                boundsOptions
            )

            val originalWidth = boundsOptions.outWidth
            val originalHeight = boundsOptions.outHeight

            if (originalWidth <= 0 || originalHeight <= 0) {
                return@withContext null
            }

            // ── Calculate sample size ─────────────────────────────────────────

            val sampleSize = calculateSampleSize(
                originalWidth,
                originalHeight,
                maxWidth,
                maxHeight
            )

            // ── Decode bitmap ─────────────────────────────────────────────────

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            var bitmap = BitmapFactory.decodeFile(
                input.absolutePath,
                decodeOptions
            ) ?: return@withContext null

            // ── Correct EXIF orientation ─────────────────────────────────────

            bitmap = correctOrientation(
                bitmap,
                input.absolutePath
            )

            // ── Final resize if needed ───────────────────────────────────────

            val finalBitmap = exactResizeIfNeeded(
                bitmap,
                maxWidth,
                maxHeight
            )

            if (finalBitmap != bitmap) {
                bitmap.recycle()
            }

            // ── Create output file ───────────────────────────────────────────

            val compressedFile = File.createTempFile(
                "IMG_CMP_",
                when (outputFormat) {
                    CompressFormat.JPEG -> ".jpg"
                    CompressFormat.PNG -> ".png"
                },
                context.cacheDir
            )

            // ── Compress bitmap ──────────────────────────────────────────────

            FileOutputStream(compressedFile).use { outputStream ->

                val format = when (outputFormat) {
                    CompressFormat.JPEG -> Bitmap.CompressFormat.JPEG
                    CompressFormat.PNG -> Bitmap.CompressFormat.PNG
                }

                val effectiveQuality =
                    if (outputFormat == CompressFormat.JPEG) {
                        quality.coerceIn(0, 100)
                    } else {
                        100
                    }

                finalBitmap.compress(
                    format,
                    effectiveQuality,
                    outputStream
                )
            }

            finalBitmap.recycle()

            compressedFile

        } catch (e: Exception) {

            if (e is CancellationException) {
                throw e
            }

            e.printStackTrace()
            null
        }
    }

    private fun calculateSampleSize(
        originalWidth: Int,
        originalHeight: Int,
        maxWidth: Int?,
        maxHeight: Int?
    ): Int {

        if (maxWidth == null && maxHeight == null) {
            return 1
        }

        val requestedWidth = maxWidth ?: originalWidth
        val requestedHeight = maxHeight ?: originalHeight

        var sampleSize = 1

        while (
            originalWidth / (sampleSize * 2) >= requestedWidth &&
            originalHeight / (sampleSize * 2) >= requestedHeight
        ) {
            sampleSize *= 2
        }

        return sampleSize
    }

    private fun correctOrientation(
        bitmap: Bitmap,
        filePath: String
    ): Bitmap {

        val exif = ExifInterface(filePath)

        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }

        val rotated = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )

        if (rotated != bitmap) {
            bitmap.recycle()
        }

        return rotated
    }

    private fun exactResizeIfNeeded(
        bitmap: Bitmap,
        maxWidth: Int?,
        maxHeight: Int?
    ): Bitmap {

        val width = bitmap.width
        val height = bitmap.height

        var newWidth = width
        var newHeight = height

        if (maxWidth != null && width > maxWidth) {
            newWidth = maxWidth
            newHeight = (height * maxWidth.toFloat() / width).toInt()
        }

        if (maxHeight != null && newHeight > maxHeight) {
            newHeight = maxHeight
            newWidth = (newWidth * maxHeight.toFloat() / newHeight).toInt()
        }

        return if (newWidth == width && newHeight == height) {
            bitmap
        } else {
            bitmap.scale(newWidth, newHeight)
        }
    }


}