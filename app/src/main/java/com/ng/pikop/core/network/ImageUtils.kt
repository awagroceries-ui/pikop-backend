package com.ng.pikop.core.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    /**
     * Compresses an image from a [Uri] and returns a new temporary [File].
     * Resizes to max 1080p and applies 75% JPEG compression.
     */
    fun compressImage(context: Context, uri: Uri, fileNamePrefix: String = "compressed"): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) return null

            // Resize logic (Max 1080 on longest side)
            val maxDimension = 1080
            val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
            val (targetWidth, targetHeight) = if (originalBitmap.width > originalBitmap.height) {
                maxDimension to (maxDimension / ratio).toInt()
            } else {
                (maxDimension * ratio).toInt() to maxDimension
            }

            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
            
            // Create temp file
            val tempFile = File(context.cacheDir, "${fileNamePrefix}_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(tempFile)
            
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            outputStream.flush()
            outputStream.close()

            resizedBitmap.recycle()
            if (originalBitmap != resizedBitmap) originalBitmap.recycle()

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
