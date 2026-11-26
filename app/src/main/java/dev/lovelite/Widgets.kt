package dev.lovelite

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import kotlin.math.max
import kotlin.math.min

/** Nền widget vẽ sẵn: bo góc mượt, gradient hồng hoặc ảnh nền phủ lớp tối nhẹ. */
object WidgetArt {
    fun background(c: Context, wDp: Int, hDp: Int, photo: Bitmap?): Bitmap {
        val d = c.resources.displayMetrics.density
        val rw = max(60, wDp) * d
        val rh = max(60, hDp) * d
        val k = min(1f, 640f / max(rw, rh))
        val w = max(2, (rw * k).toInt())
        val h = max(2, (rh * k).toInt())
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())
        val r = 22f * d * k
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        if (photo != null) {
            val sh = BitmapShader(photo, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val s = max(w / photo.width.toFloat(), h / photo.height.toFloat())
            val m = Matrix()
            m.setScale(s, s)
            m.postTranslate((w - photo.width * s) / 2f, (h - photo.height * s) / 2f)
            sh.setLocalMatrix(m)
            p.shader = sh
            cv.drawRoundRect(rect, r, r, p)
            p.shader = null
            p.color = Color.parseColor("#33D81B60")
            cv.drawRoundRect(rect, r, r, p)
            p.shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                Color.parseColor("#55000000"), Color.parseColor("#AA000000"), Shader.TileMode.CLAMP,
            )
            cv.drawRoundRect(rect, r, r, p)
        } else {
            p.shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                Color.parseColor("#F06292"), Color.parseColor("#B0125B"), Shader.TileMode.CLAMP,
            )
            cv.drawRoundRect(rect, r, r, p)
            // Hai vòng tròn mờ làm điểm nhấn.
            p.shader = null
            p.color = Color.parseColor("#1AFFFFFF")
            cv.save()
            cv.clipRect(rect)
            cv.drawCircle(w * 0.95f, h * 0.05f, h * 0.75f, p)
            cv.drawCircle(w * 0.02f, h * 1.0f, h * 0.55f, p)
            cv.restore()
        }
        return bmp
    }
}

object Widgets {
    private class Data(c: Context) {
        val ctx: Context = Lang.wrap(c)
        val p = arrayOf(Store.person(ctx, 0), Store.person(ctx, 1))
        val start = Store.start(ctx)
        val photo: Bitmap? = if (Store.widgetPhoto(ctx)) Photo.loadBg(ctx) else null
        val av = Array(2) { Photo.circle(Photo.loadAvatar(ctx, it), 128, Info.initial(p[it]), p[it].gender) }
        val msg: String = Greetings.get(ctx).body
    }

    fun refresh(c: Context) {
        val m = AppWidgetManager.getInstance(c)
        val wide = m.getAppWidgetIds(ComponentName(c, WideWidget::class.java))
        val small = m.getAppWidgetIds(ComponentName(c, SmallWidget::class.java))
        if (wide.isEmpty() && small.isEmpty()) return
        val d = Data(c)
        for (id in wide) m.updateAppWidget(id, build(d, m, id, true))
        for (id in small) m.updateAppWidget(id, build(d, m, id, false))
    }

    fun update(c: Context, m: AppWidgetManager, id: Int, wide: Boolean) {
        m.updateAppWidget(id, build(Data(c), m, id, wide))
    }

    private fun build(d: Data, m: AppWidgetManager, id: Int, wide: Boolean): RemoteViews {
        val c = d.ctx
        val v = RemoteViews(c.packageName, if (wide) R.layout.widget_wide else R.layout.widget_small)
        val o = m.getAppWidgetOptions(id)
        val wDp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, if (wide) 250 else 110)
        val hDp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
        v.setImageViewBitmap(R.id.w_bg, WidgetArt.background(c, wDp, hDp, d.photo))
        v.setImageViewBitmap(R.id.w_av_a, d.av[0])
        v.setImageViewBitmap(R.id.w_av_b, d.av[1])

        val s = d.start
        if (s == null) {
            v.setTextViewText(R.id.w_days, "♥")
            v.setTextViewText(R.id.w_label, c.getString(R.string.tap_setup))
        } else {
            v.setTextViewText(R.id.w_days, Milestones.daysTogether(s).toString())
            v.setTextViewText(R.id.w_label, c.getString(R.string.days_together))
        }

        if (wide) {
            for (i in 0..1) {
                val nameId = if (i == 0) R.id.w_name_a else R.id.w_name_b
                val metaId = if (i == 0) R.id.w_meta_a else R.id.w_meta_b
                v.setTextViewText(nameId, Info.name(c, i, d.p[i]))
                val meta = Info.short(c, d.p[i])
                v.setTextViewText(metaId, meta)
                v.setViewVisibility(metaId, if (meta.isEmpty()) View.GONE else View.VISIBLE)
            }
            v.setTextViewText(R.id.w_msg, d.msg)
        }

        val open = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        v.setOnClickPendingIntent(R.id.w_root, open)
        return v
    }
}

/** Widget cập nhật khi qua ngày mới, đổi giờ/múi giờ, đổi kích thước hoặc khi người dùng sửa dữ liệu. */
open class BaseWidget(private val wide: Boolean) : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) = Widgets.refresh(c)

    override fun onAppWidgetOptionsChanged(c: Context, m: AppWidgetManager, id: Int, newOptions: Bundle) {
        super.onAppWidgetOptionsChanged(c, m, id, newOptions)
        Widgets.update(c, m, id, wide)
    }

    override fun onReceive(c: Context, i: Intent) {
        super.onReceive(c, i)
        when (i.action) {
            Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> Widgets.refresh(c)
        }
    }
}

class WideWidget : BaseWidget(true)
class SmallWidget : BaseWidget(false)
