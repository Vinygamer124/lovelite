package dev.lovelite

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import kotlin.math.max

/** Ảnh nền (≤1080px) và ảnh đại diện (vuông 512px) lưu JPEG: nhẹ bộ nhớ và dung lượng. */
object Photo {
    private const val BG_MAX = 1080
    private const val AV_SIZE = 512

    fun bgFile(c: Context) = File(c.filesDir, "bg.jpg")
    fun avatarFile(c: Context, i: Int) = File(c.filesDir, if (i == 0) "avatar_a.jpg" else "avatar_b.jpg")

    /** Đọc ảnh, giảm mẫu, xoay theo EXIF. maxSide là cạnh dài tối thiểu cần giữ. */
    private fun decode(c: Context, uri: Uri, maxSide: Int): Bitmap? {
        val r = c.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        r.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var s = 1
        while (max(bounds.outWidth, bounds.outHeight) / (s * 2) >= maxSide) s *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = s }
        val bmp = r.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val orient = try {
            r.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) } ?: 1
        } catch (e: Exception) { 1 }
        val deg = when (orient) { 3 -> 180f; 6 -> 90f; 8 -> 270f; else -> 0f }
        return if (deg == 0f) bmp else
            Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(deg) }, true)
    }

    fun saveBg(c: Context, uri: Uri): Boolean = try {
        var bmp = decode(c, uri, BG_MAX) ?: return false
        val m = max(bmp.width, bmp.height)
        if (m > BG_MAX) {
            val k = BG_MAX / m.toFloat()
            bmp = Bitmap.createScaledBitmap(bmp, (bmp.width * k).toInt(), (bmp.height * k).toInt(), true)
        }
        bgFile(c).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        true
    } catch (e: Exception) { false }

    /** Cắt vuông ở giữa rồi thu về 512px. */
    fun saveAvatar(c: Context, i: Int, uri: Uri): Boolean = try {
        val bmp = decode(c, uri, AV_SIZE) ?: return false
        val side = minOf(bmp.width, bmp.height)
        val sq = Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
        val out = if (side > AV_SIZE) Bitmap.createScaledBitmap(sq, AV_SIZE, AV_SIZE, true) else sq
        avatarFile(c, i).outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        true
    } catch (e: Exception) { false }

    fun loadBg(c: Context): Bitmap? = load(bgFile(c))
    fun loadAvatar(c: Context, i: Int): Bitmap? = load(avatarFile(c, i))

    private fun load(f: File): Bitmap? =
        if (!f.exists()) null else BitmapFactory.decodeFile(f.path)

    /** Ảnh tròn có viền trắng; nếu chưa có ảnh thì vẽ nền gradient theo giới tính + chữ cái đầu. */
    fun circle(src: Bitmap?, size: Int, initial: String, gender: Int): Bitmap {
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val cv = Canvas(out)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val ring = size * 0.045f
        val cx = size / 2f
        p.color = Color.WHITE
        cv.drawCircle(cx, cx, cx, p)
        val r = cx - ring
        if (src != null) {
            val sh = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val k = (2 * r) / minOf(src.width, src.height)
            val m = Matrix()
            m.setScale(k, k)
            m.postTranslate(ring - (src.width * k - 2 * r) / 2f, ring - (src.height * k - 2 * r) / 2f)
            sh.setLocalMatrix(m)
            p.shader = sh
            cv.drawCircle(cx, cx, r, p)
        } else {
            val (c1, c2) = when (gender) {
                1 -> Color.parseColor("#82AAF5") to Color.parseColor("#4F7BDC")
                3 -> Color.parseColor("#B39DDB") to Color.parseColor("#7E57C2")
                else -> Color.parseColor("#F48FB1") to Color.parseColor("#D81B60")
            }
            p.shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), c1, c2, Shader.TileMode.CLAMP)
            cv.drawCircle(cx, cx, r, p)
            p.shader = null
            p.color = Color.WHITE
            p.textAlign = Paint.Align.CENTER
            p.isFakeBoldText = true
            p.textSize = size * 0.42f
            cv.drawText(initial, cx, cx - (p.descent() + p.ascent()) / 2f, p)
        }
        return out
    }
}
