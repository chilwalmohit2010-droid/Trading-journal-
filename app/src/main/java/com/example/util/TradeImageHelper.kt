package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * Helper to persist chart screenshots (gallery picker or camera)
 * safely to internal application storage so they remain accessible permanently
 * across app restarts without permission expiration.
 */
object TradeImageHelper {

    private const val CHARTS_DIR = "trade_charts"
    private const val CAMERA_DIR = "camera_photos"

    private fun getChartsDirectory(context: Context): File {
        val dir = File(context.filesDir, CHARTS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getCameraDirectory(context: Context): File {
        val dir = File(context.cacheDir, CAMERA_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Copies an image from a content URI (e.g. Photo Picker) to app internal storage.
     * Returns the permanent file Uri string.
     */
    fun copyUriToInternalStorage(context: Context, sourceUri: Uri, prefix: String = "chart"): String? {
        return try {
            val dir = getChartsDirectory(context)
            val fileName = "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
            val targetFile = File(dir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input: InputStream ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(targetFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves a captured Bitmap from camera to app internal storage.
     * Returns the permanent file Uri string.
     */
    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap, prefix: String = "cam_chart"): String? {
        return try {
            val dir = getChartsDirectory(context)
            val fileName = "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
            val targetFile = File(dir, fileName)

            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            Uri.fromFile(targetFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Creates a temporary file and returns its content Uri via FileProvider for full-resolution camera captures.
     */
    fun createTempCameraUri(context: Context): Pair<Uri, File>? {
        return try {
            val dir = getCameraDirectory(context)
            val file = File(dir, "temp_capture_${System.currentTimeMillis()}.jpg")
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            Pair(uri, file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
