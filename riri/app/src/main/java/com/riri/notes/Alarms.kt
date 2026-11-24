package com.riri.notes

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.json.JSONObject
import java.util.Calendar

/**
 * Doğum günü hatırlatmaları. Her kişi için en fazla üç alarm kurulur:
 * 7 gün önce, 1 gün önce ve günün kendisi, hepsi saat 09:00'da.
 */
object Alarms {
    const val CHANNEL = "birthdays"
    private val OFFSETS = intArrayOf(7, 1, 0)

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            val ch = NotificationChannel(CHANNEL, "Doğum günleri / Birthdays", NotificationManager.IMPORTANCE_HIGH)
            ch.enableVibration(true)
            nm.createNotificationChannel(ch)
        }
    }

    fun scheduleAll(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val prefs = ctx.getSharedPreferences("riri", Context.MODE_PRIVATE)
        // Önce eski alarmları iptal et.
        val old = prefs.getString("alarm_codes", "") ?: ""
        old.split(',').filter { it.isNotBlank() }.forEach { code ->
            am.cancel(pending(ctx, code.toInt(), Intent(ctx, BirthdayReceiver::class.java)))
        }
        val raw = prefs.getString("birthdays", "") ?: ""
        if (raw.isBlank()) return
        val data = try { JSONObject(raw) } catch (e: Exception) { return }
        if (!data.optBoolean("enabled", true)) { prefs.edit().putString("alarm_codes", "").apply(); return }
        val flags = booleanArrayOf(data.optBoolean("week", true), data.optBoolean("day", true), data.optBoolean("onDay", true))
        val texts = data.optJSONObject("texts") ?: JSONObject()
        val people = data.optJSONArray("people") ?: return
        val codes = mutableListOf<Int>()
        val now = System.currentTimeMillis()
        for (i in 0 until people.length()) {
            val p = people.getJSONObject(i)
            val name = p.optString("name")
            val parts = p.optString("date").split('-')
            if (parts.size != 3) continue
            val month = parts[1].toIntOrNull() ?: continue
            val day = parts[2].toIntOrNull() ?: continue
            OFFSETS.forEachIndexed { idx, off ->
                if (!flags[idx]) return@forEachIndexed
                val cal = nextOccurrence(month, day, off, now) ?: return@forEachIndexed
                val key = when (off) { 7 -> "week"; 1 -> "day"; else -> "today" }
                val body = texts.optString(key, "%s").replace("%s", name)
                val code = (p.optInt("id") * 10 + idx) and 0x7fffffff
                val intent = Intent(ctx, BirthdayReceiver::class.java)
                    .putExtra("title", texts.optString("title", "Riri's Notes"))
                    .putExtra("body", body)
                    .putExtra("code", code)
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending(ctx, code, intent))
                codes += code
            }
        }
        prefs.edit().putString("alarm_codes", codes.joinToString(",")).apply()
    }

    private fun nextOccurrence(month: Int, day: Int, daysBefore: Int, now: Long): Calendar? {
        val today = Calendar.getInstance()
        for (yearAdd in 0..1) {
            val c = Calendar.getInstance()
            c.set(today.get(Calendar.YEAR) + yearAdd, month - 1, day, 9, 0, 0)
            c.set(Calendar.MILLISECOND, 0)
            c.add(Calendar.DAY_OF_YEAR, -daysBefore)
            if (c.timeInMillis > now) return c
        }
        return null
    }

    private fun pending(ctx: Context, code: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(ctx, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}
