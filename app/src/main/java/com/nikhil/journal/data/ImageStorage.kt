package com.nikhil.journal.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Copies picked/captured images into app-private storage (`filesDir/images/`), downscaled so
 * the long edge is at most [MAX_EDGE_PX] and re-encoded as JPEG ([JPEG_QUALITY]%).
 *
 * Paths handed out are relative to `filesDir` so they survive a device-to-device transfer.
 */
class ImageStorage(context: Context) {

    private val appContext = context.applicationContext
    private val filesDir: File = appContext.filesDir
    private val imagesDir: File get() = File(filesDir, IMAGES_DIR).apply { mkdirs() }
    private val cameraDir: File get() = File(appContext.cacheDir, CAMERA_DIR).apply { mkdirs() }

    fun resolve(relativePath: String): File = File(filesDir, relativePath)

    /** A fresh temp file for the camera app to write into (exposed via FileProvider). */
    fun newCameraTempFile(): File = File(cameraDir, "capture_${UUID.randomUUID()}.jpg")

    /**
     * Decodes [uri], downscales, fixes EXIF rotation and saves it privately.
     * Returns the relative path, or null if the image could not be read.
     */
    suspend fun import(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val bitmap = decodeScaled(uri) ?: return@withContext null
            val name = "${UUID.randomUUID()}.jpg"
            val target = File(imagesDir, name)
            val tmp = File(imagesDir, "$name.tmp")
            tmp.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            bitmap.recycle()
            if (!tmp.renameTo(target)) {
                tmp.delete()
                return@withContext null
            }
            "$IMAGES_DIR/$name"
        } catch (e: Exception) {
            Log.w(TAG, "Failed to import image", e)
            null
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "Image too large to import", e)
            null
        }
    }

    /** Imports a camera capture and removes the temporary file. */
    suspend fun importCameraCapture(tempFile: File, uri: Uri): String? {
        return try {
            import(uri)
        } finally {
            withContext(Dispatchers.IO) { tempFile.delete() }
        }
    }

    suspend fun delete(relativePaths: Collection<String>) = withContext(Dispatchers.IO) {
        relativePaths.forEach { resolve(it).delete() }
    }

    private fun decodeScaled(uri: Uri): Bitmap? {
        val resolver = appContext.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_EDGE_PX)
        }
        val sampled = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val rotation = try {
            resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        } catch (e: Exception) {
            0
        }

        val (targetW, targetH) = targetSize(sampled.width, sampled.height, MAX_EDGE_PX)
        val matrix = Matrix().apply {
            if (targetW != sampled.width) {
                postScale(targetW / sampled.width.toFloat(), targetH / sampled.height.toFloat())
            }
            if (rotation != 0) postRotate(rotation.toFloat())
        }
        val transformed = if (matrix.isIdentity) {
            sampled
        } else {
            Bitmap.createBitmap(sampled, 0, 0, sampled.width, sampled.height, matrix, true)
                .also { if (it !== sampled) sampled.recycle() }
        }
        return flattenAlpha(transformed)
    }

    /** JPEG has no alpha; paint transparent images (e.g. PNG screenshots) onto white. */
    private fun flattenAlpha(bitmap: Bitmap): Bitmap {
        if (!bitmap.hasAlpha()) return bitmap
        val opaque = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Canvas(opaque).apply {
            drawColor(Color.WHITE)
            drawBitmap(bitmap, 0f, 0f, null)
        }
        bitmap.recycle()
        return opaque
    }

    companion object {
        private const val TAG = "ImageStorage"
        const val IMAGES_DIR = "images"
        private const val CAMERA_DIR = "camera"
        const val MAX_EDGE_PX = 1600
        const val JPEG_QUALITY = 85

        /**
         * Largest power-of-two sample size that still leaves the long edge >= [maxEdge],
         * so huge photos are never fully decoded into memory.
         */
        fun calculateInSampleSize(width: Int, height: Int, maxEdge: Int): Int {
            val longEdge = max(width, height)
            var sample = 1
            while (longEdge / (sample * 2) >= maxEdge) sample *= 2
            return sample
        }

        /** Final size with the long edge capped at [maxEdge], preserving aspect ratio. */
        fun targetSize(width: Int, height: Int, maxEdge: Int): Pair<Int, Int> {
            val longEdge = max(width, height)
            if (longEdge <= maxEdge) return width to height
            val scale = maxEdge / longEdge.toFloat()
            return max(1, (width * scale).roundToInt()) to max(1, (height * scale).roundToInt())
        }
    }
}
