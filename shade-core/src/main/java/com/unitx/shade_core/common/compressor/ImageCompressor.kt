package com.unitx.shade_core.common.compressor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import com.unitx.shade_core.common.config.extend.ProgressConfig
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

    suspend fun compress(
        context: Context,
        inputs: List<File>,
        quality: Int,
        maxWidth: Int?,
        maxHeight: Int?,
        onProgress: ((ProgressConfig.Compressing) -> Unit)?,
        outputFormat: CompressFormat = CompressFormat.JPEG
    ): List<Result<File>> {
        return inputs.mapIndexed { index, file ->
            compress(
                context = context,
                input = file,
                quality = quality,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
                onProgress = onProgress,
                fileNumber = index + 1,
                outputFormat = outputFormat
            )
        }
    }

    suspend fun compress(
        context: Context,
        input: File,
        quality: Int,
        maxWidth: Int?,
        maxHeight: Int?,
        onProgress: ((ProgressConfig.Compressing) -> Unit)?,
        fileNumber: Int = 1,
        outputFormat: CompressFormat = CompressFormat.JPEG
    ): Result<File> = withContext(Dispatchers.IO) {

        try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(input.absolutePath, boundsOptions)

            val originalWidth = boundsOptions.outWidth
            val originalHeight = boundsOptions.outHeight

            if (originalWidth <= 0 || originalHeight <= 0) {
                return@withContext Result.failure(
                    IllegalStateException("Invalid image dimensions: ${originalWidth}x${originalHeight} for file ${input.name}")
                )
            }

            onProgress?.invoke(ProgressConfig.Compressing(10, fileNumber))

            val sampleSize = calculateSampleSize(originalWidth, originalHeight, maxWidth, maxHeight)

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            var bitmap = BitmapFactory.decodeFile(input.absolutePath, decodeOptions)
                ?: return@withContext Result.failure(
                    IllegalStateException("BitmapFactory returned null for file ${input.name}")
                )

            onProgress?.invoke(ProgressConfig.Compressing(40, fileNumber))

            bitmap = correctOrientation(bitmap, input.absolutePath)

            onProgress?.invoke(ProgressConfig.Compressing(60, fileNumber))

            val finalBitmap = exactResizeIfNeeded(bitmap, originalWidth, originalHeight, maxWidth, maxHeight)
            if (finalBitmap != bitmap) bitmap.recycle()

            onProgress?.invoke(ProgressConfig.Compressing(75, fileNumber))

            val compressedFile = File.createTempFile(
                "IMG_CMP_",
                when (outputFormat) {
                    CompressFormat.JPEG -> ".jpg"
                    CompressFormat.PNG -> ".png"
                },
                context.cacheDir
            )

            FileOutputStream(compressedFile).use { outputStream ->
                val format = when (outputFormat) {
                    CompressFormat.JPEG -> Bitmap.CompressFormat.JPEG
                    CompressFormat.PNG -> Bitmap.CompressFormat.PNG
                }
                val effectiveQuality =
                    if (outputFormat == CompressFormat.JPEG) quality.coerceIn(0, 100) else 100
                finalBitmap.compress(format, effectiveQuality, outputStream)
            }

            finalBitmap.recycle()

            onProgress?.invoke(ProgressConfig.Compressing(100, fileNumber))

            Result.success(compressedFile)

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    private fun calculateSampleSize(
        originalWidth: Int,
        originalHeight: Int,
        maxWidth: Int?,
        maxHeight: Int?
    ): Int {
        if (maxWidth == null && maxHeight == null) return 1

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

    private fun correctOrientation(bitmap: Bitmap, filePath: String): Bitmap {
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

        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    private fun exactResizeIfNeeded(
        bitmap: Bitmap,
        originalWidth: Int,
        originalHeight: Int,
        maxWidth: Int?,
        maxHeight: Int?
    ): Bitmap {
        var newWidth = originalWidth
        var newHeight = originalHeight

        if (maxWidth != null && newWidth > maxWidth) {
            newWidth = maxWidth
            newHeight = (originalHeight * maxWidth.toFloat() / originalWidth).toInt()
        }

        if (maxHeight != null && newHeight > maxHeight) {
            newHeight = maxHeight
            newWidth = (originalWidth * maxHeight.toFloat() / originalHeight).toInt()
        }

        return if (newWidth == originalWidth && newHeight == originalHeight) {
            bitmap
        } else {
            bitmap.scale(newWidth, newHeight)
        }
    }
}