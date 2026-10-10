package com.modernqr.app.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_IMAGE_DIMENSION = 1600

suspend fun decodeGalleryBarcode(context: Context, uri: Uri): Result = withContext(Dispatchers.IO) {
    try {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: throw GalleryDecodeException("The selected image could not be opened.")

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw GalleryDecodeException("The selected file is not a valid image.")
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw GalleryDecodeException("The selected image could not be decoded.")

        try {
            decodeWithRotations(bitmap)
        } finally {
            bitmap.recycle()
        }
    } catch (error: OutOfMemoryError) {
        throw GalleryDecodeException("The selected image is too large to scan safely.", error)
    }
}

private fun calculateSampleSize(width: Int, height: Int): Int {
    var sampleSize = 1
    while (width / sampleSize > MAX_IMAGE_DIMENSION || height / sampleSize > MAX_IMAGE_DIMENSION) {
        sampleSize *= 2
    }
    return sampleSize
}

private fun decodeWithRotations(bitmap: Bitmap): Result {
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    var source: LuminanceSource = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)

    repeat(4) { attempt ->
        try {
            return MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source)))
        } catch (_: NotFoundException) {
            if (attempt < 3 && source.isRotateSupported) source = source.rotateCounterClockwise()
        }
    }
    throw GalleryDecodeException("No supported barcode was found in the selected image.")
}

class GalleryDecodeException(message: String, cause: Throwable? = null) : Exception(message, cause)
