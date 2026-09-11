package com.dockeredly.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Turns a user-picked image or a downloaded website icon into a single square PNG stored
 * under the app's files dir, at the sizes the launcher UI actually needs. Never executes
 * icon content (SVG rasterization, if ever added, would need a sandboxed renderer) - only
 * raster formats decodable by [BitmapFactory] are accepted.
 */
object IconProcessor {
    private const val ICON_DIR = "webapp_icons"
    private const val TARGET_SIZE_PX = 192

    private fun iconDir(context: Context): File =
        File(context.filesDir, ICON_DIR).apply { mkdirs() }

    suspend fun processUri(context: Context, uri: Uri, webAppId: String): String? =
        withContext(Dispatchers.IO) {
            val bitmap = decodeUri(context, uri) ?: return@withContext null
            saveSquareIcon(context, bitmap, webAppId)
        }

    suspend fun processBytes(context: Context, bytes: ByteArray, webAppId: String): String? =
        withContext(Dispatchers.IO) {
            val bitmap = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
                ?: return@withContext null
            saveSquareIcon(context, bitmap, webAppId)
        }

    private fun decodeUri(context: Context, uri: Uri): Bitmap? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    }.getOrNull()

    private fun saveSquareIcon(context: Context, source: Bitmap, webAppId: String): String? {
        val cropped = cropToSquare(source)
        val scaled = Bitmap.createScaledBitmap(cropped, TARGET_SIZE_PX, TARGET_SIZE_PX, true)
        val file = File(iconDir(context), "$webAppId.png")
        return runCatching {
            FileOutputStream(file).use { out -> scaled.compress(Bitmap.CompressFormat.PNG, 100, out) }
            file.absolutePath
        }.getOrNull()
    }

    private fun cropToSquare(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        return Bitmap.createBitmap(bitmap, x, y, size, size)
    }

    fun deleteIcon(webAppId: String, context: Context) {
        File(iconDir(context), "$webAppId.png").delete()
    }
}
