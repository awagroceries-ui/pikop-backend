package com.ng.pikop.core.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageUtils {

    /**
     * Reads EXIF orientation from a [Uri] or [File] and rotates the [Bitmap] accordingly.
     */
    private fun rotateBitmapIfRequired(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        var inputStream: InputStream? = null
        val exif = try {
            if (uri.scheme == "file") {
                uri.path?.let { ExifInterface(it) }
            } else {
                inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    ExifInterface(inputStream)
                } else null
            }
        } catch (_: Exception) {
            null
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
        } ?: return bitmap

        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }

        return try {
            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            if (rotatedBitmap != bitmap) {
                bitmap.recycle()
            }
            rotatedBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Compresses an image from a [Uri] and returns a new temporary [File].
     * Resizes to max 1080p, fixes EXIF orientation, and applies 75% JPEG compression.
     */
    fun compressImage(context: Context, uri: Uri, fileNamePrefix: String = "compressed"): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return null
            val rawBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (rawBitmap == null) return null

            // Rotate according to EXIF orientation metadata
            val orientedBitmap = rotateBitmapIfRequired(context, uri, rawBitmap)

            // Resize logic (Max 1080 on longest side)
            val maxDimension = 1080
            val ratio = orientedBitmap.width.toFloat() / orientedBitmap.height.toFloat()
            val (targetWidth, targetHeight) = if (orientedBitmap.width > orientedBitmap.height) {
                maxDimension to (maxDimension / ratio).toInt().coerceAtLeast(1)
            } else {
                (maxDimension * ratio).toInt().coerceAtLeast(1) to maxDimension
            }

            val resizedBitmap = if (orientedBitmap.width > maxDimension || orientedBitmap.height > maxDimension) {
                Bitmap.createScaledBitmap(orientedBitmap, targetWidth, targetHeight, true)
            } else {
                orientedBitmap
            }

            // Create temp file
            val tempFile = File(context.cacheDir, "${fileNamePrefix}_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(tempFile)
            
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            outputStream.flush()
            outputStream.close()

            if (resizedBitmap != orientedBitmap) resizedBitmap.recycle()
            if (orientedBitmap != rawBitmap) orientedBitmap.recycle()
            rawBitmap.recycle()

            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Compresses an existing [File] in place or returns a new compressed [File].
     */
    fun compressFile(context: Context, file: File): File {
        val compressedFile = compressImage(context, Uri.fromFile(file), "upload") ?: file
        return compressedFile
    }
}
