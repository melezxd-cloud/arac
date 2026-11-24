package com.riri.notes

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.json.JSONObject
import java.util.Calendar

/** Ana ekran widget'ı: saat, tarih, en yakın doğum gününe kalan gün ve sıradaki görev. */
class RiriWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { mgr.updateAppWidget(it, build(ctx)) }
    }

    companion object {
        fun refreshAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, RiriWidget::class.java))
            ids.forEach { mgr.updateAppWidget(it, build(ctx)) }
        }

        private fun build(ctx: Context): RemoteViews {
            val v = RemoteViews(ctx.packageName, R.layout.widget_riri)
            val raw = ctx.getSharedPreferences("riri", Context.MODE_PRIVATE).getString("widget", "") ?: ""
            val d = try { JSONObject(raw) } catch (e: Exception) { JSONObject() }

            val days = daysUntil(d.optString("bdDate"))
            if (days != null) {
                v.setTextViewText(R.id.w_days, days.toString())
                v.setTextViewText(R.id.w_days_label, d.optString("daysLabel", "gün"))
                v.setTextViewText(R.id.w_bd, d.optString("bdLine", ""))
            } else {
                v.setTextViewText(R.id.w_days, "–")
                v.setTextViewText(R.id.w_days_label, "")
                v.setTextViewText(R.id.w_bd, d.optString("noBd", ""))
            }
            v.setTextViewText(R.id.w_todo, d.optString("todoLine", ""))

            val open = PendingIntent.getActivity(
                ctx, 1, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            v.setOnClickPendingIntent(R.id.w_root, open)
            return v
        }

        private fun daysUntil(date: String): Int? {
            val p = date.split('-')
            if (p.size != 3) return null
            val m = p[1].toIntOrNull() ?: return null
            val dd = p[2].toIntOrNull() ?: return null
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val b = Calendar.getInstance().apply {
                set(today.get(Calendar.YEAR), m - 1, dd, 0, 0, 0); set(Calendar.MILLISECOND, 0)
            }
            if (b.before(today)) b.add(Calendar.YEAR, 1)
            return ((b.timeInMillis - today.timeInMillis) / 86_400_000L).toInt()
        }
    }
}
