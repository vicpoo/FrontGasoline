package org.vicpoo.gasolineproject.core.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

/** Reduce fotos a JPEG de máximo 1920 px por lado, calidad 85 (un ticket queda legible y pesa < 1 MB). */
class AndroidImageCompressor : ImageCompressor {

    override suspend fun compress(file: PickedFile): PickedFile {
        if (!file.mimeType.startsWith("image/")) return file
        val bytes = withContext(Dispatchers.Default) { runCatching { compressBytes(file.bytes) }.getOrNull() }
            ?: return file
        if (bytes.size >= file.bytes.size && file.mimeType == "image/jpeg") return file
        val baseName = file.name.substringBeforeLast('.', file.name)
        return PickedFile(name = "$baseName.jpg", mimeType = "image/jpeg", bytes = bytes)
    }

    companion object {
        private const val MAX_SIDE = 1920
        private const val QUALITY = 85

        fun compressFile(file: File): ByteArray = compressBytes(file.readBytes())

        fun compressBytes(original: ByteArray): ByteArray {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(original, 0, original.size, bounds)
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2

            val decoded = BitmapFactory.decodeByteArray(
                original,
                0,
                original.size,
                BitmapFactory.Options().apply { inSampleSize = sample },
            ) ?: error("No es una imagen")

            val rotated = decoded.rotateByExif(original)
            val scaled = rotated.scaleDown(MAX_SIDE)
            return ByteArrayOutputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
                out.toByteArray()
            }
        }

        private fun Bitmap.scaleDown(maxSide: Int): Bitmap {
            val largest = max(width, height)
            if (largest <= maxSide) return this
            val ratio = maxSide.toFloat() / largest
            return Bitmap.createScaledBitmap(this, (width * ratio).toInt(), (height * ratio).toInt(), true)
        }

        /** Las cámaras guardan la orientación en EXIF; se aplica para que el ticket no salga de lado. */
        private fun Bitmap.rotateByExif(original: ByteArray): Bitmap {
            val orientation = runCatching {
                ExifInterface(ByteArrayInputStream(original))
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> return this
            }
            val matrix = Matrix().apply { postRotate(degrees) }
            return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
        }
    }
}
