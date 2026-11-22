package dev.lovelite

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

object Widgets {
    fun refresh(c: Context) {
        val m = AppWidgetManager.getInstance(c)
        val kinds = listOf(
            WideWidget::class.java to R.layout.widget_wide,
            SmallWidget::class.java to R.layout.widget_small,
        )
        for ((cls, layout) in kinds) {
            val ids = m.getAppWidgetIds(ComponentName(c, cls))
            if (ids.isNotEmpty()) m.updateAppWidget(ids, build(c, layout))
        }
    }

    private fun build(c: Context, layout: Int): RemoteViews {
        val v = RemoteViews(c.packageName, layout)
        val (a, b) = Store.names(c)
        val start = Store.start(c)
        if (start == null) {
            v.setTextViewText(R.id.w_days, "♥")
            v.setTextViewText(R.id.w_names, c.getString(R.string.tap_setup))
        } else {
            v.setTextViewText(R.id.w_days, Milestones.daysTogether(start).toString())
            v.setTextViewText(R.id.w_names, listOf(a, b).filter { it.isNotBlank() }.joinToString(" ♥ "))
        }
        val open = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        v.setOnClickPendingIntent(R.id.w_root, open)
        return v
    }
}

/** Widget chỉ cập nhật khi cần: lúc qua ngày mới, đổi giờ/múi giờ, hoặc khi người dùng sửa dữ liệu. */
open class BaseWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) = Widgets.refresh(c)

    override fun onReceive(c: Context, i: Intent) {
        super.onReceive(c, i)
        when (i.action) {
            Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> Widgets.refresh(c)
        }
    }
}

class WideWidget : BaseWidget()
class SmallWidget : BaseWidget()
