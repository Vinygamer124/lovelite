package dev.lovelite

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File

/** Thu nhỏ ảnh nền về tối đa ~1080px rồi lưu JPEG: nhẹ bộ nhớ và dung lượng. */
object Photo {
    private const val MAX = 1080

    fun file(c: Context) = File(c.filesDir, "bg.jpg")

    fun save(c: Context, uri: Uri): Boolean {
        return try {
            val r = c.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            r.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var s = 1
            while (bounds.outWidth / (s * 2) >= MAX) s *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = s }
            val bmp = r.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return false
            val orient = r.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
            } ?: 1
            val deg = when (orient) { 3 -> 180f; 6 -> 90f; 8 -> 270f; else -> 0f }
            val out = if (deg == 0f) bmp else
                Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(deg) }, true)
            file(c).outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            true
        } catch (e: Exception) { false }
    }

    fun load(c: Context): Bitmap? {
        val f = file(c)
        if (!f.exists()) return null
        return BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565
        })
    }
}
