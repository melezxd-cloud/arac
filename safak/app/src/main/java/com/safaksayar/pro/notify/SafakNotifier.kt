package com.safaksayar.pro.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.safaksayar.pro.MainActivity
import com.safaksayar.pro.R
import com.safaksayar.pro.data.Plates
import com.safaksayar.pro.data.SafakCalculator
import com.safaksayar.pro.data.SafakResult
import com.safaksayar.pro.data.SettingsRepository
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * - Durum çubuğunda kalan gün sayısını gösteren ikon
 * - Bildirim perdesinde kalıcı "Şafak N" çubuğu (ilerleme çubuklu)
 * - Özel gün bildirimleri (100, 81, 30, 7, 1, terhis, acemilik bitişi)
 * - Her gece yarısı kendini günceller
 */
object SafakNotifier {
    private const val CH_STATUS = "safak_status"
    private const val CH_MILESTONE = "safak_milestone"
    private const val ID_STATUS = 1
    private const val ID_MILESTONE = 2
    private const val RED = 0xFFC8102E.toInt()
    private val DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_STATUS, context.getString(R.string.channel_status), NotificationManager.IMPORTANCE_LOW)
                .apply { description = context.getString(R.string.channel_status_desc); setShowBadge(false) }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_MILESTONE, context.getString(R.string.channel_milestone), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.channel_milestone_desc) }
        )
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Ayarları okur, bildirimi günceller, özel günü kontrol eder ve bir sonraki gece yarısını kurar. */
    suspend fun refresh(context: Context) {
        createChannels(context)
        val repo = SettingsRepository(context)
        val settings = repo.current()
        val today = LocalDate.now()
        val result = SafakCalculator.compute(settings, today)
        val nm = NotificationManagerCompat.from(context)

        if (result == null || !canPost(context)) {
            nm.cancel(ID_STATUS)
        } else {
            if (settings.notificationEnabled) postStatus(context, result) else nm.cancel(ID_STATUS)
            checkMilestone(context, repo, result, today)
        }
        scheduleNextMidnight(context)
    }

    @Suppress("MissingPermission")
    private fun postStatus(context: Context, r: SafakResult) {
        val title = if (r.finished) "Terhis oldun!" else "Şafak ${r.daysLeft}"
        val stage = when {
            !r.ustaStarted -> "Acemi birliği · ${r.acemiLeft} gün kaldı"
            r.finished -> "Hayırlı olsun"
            else -> "Usta birliği devam ediyor"
        }
        val city = Plates.cityFor(r.daysLeft)
        val n = NotificationCompat.Builder(context, CH_STATUS)
            .setSmallIcon(numberIcon(r.daysLeft))
            .setColor(RED)
            .setContentTitle(title)
            .setContentText("Terhis: ${r.end.format(DATE)} · %${r.percent} tamamlandı")
            .setSubText(city?.let { "${Plates.code(r.daysLeft)} $it" })
            .setStyle(NotificationCompat.BigTextStyle().bigText("Terhis: ${r.end.format(DATE)} · %${r.percent} tamamlandı\n$stage"))
            .setProgress(r.totalDays, r.daysPassed, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openApp(context))
            .build()
        NotificationManagerCompat.from(context).notify(ID_STATUS, n)
    }

    @Suppress("MissingPermission")
    private suspend fun checkMilestone(context: Context, repo: SettingsRepository, r: SafakResult, today: LocalDate) {
        val msg: Pair<String, String>? = when {
            r.finished && today == r.end -> "Terhis oldun!" to "Şafak bitti, hayırlı olsun!"
            r.acemiLeft == 0 && today == r.acemiEnd -> "Acemilik bitti" to "Usta birliği başladı. Şafak ${r.daysLeft}."
            r.daysLeft == 100 -> "Şafak 100" to "Üç haneli son gün!"
            r.daysLeft == 81 -> "Şafak 81 · DÜZCE" to "Plaka günleri başladı!"
            r.daysLeft == 30 -> "Şafak 30 · ${Plates.cityFor(30)}" to "Son 30 gün!"
            r.daysLeft == 7 -> "Şafak 7 · ${Plates.cityFor(7)}" to "Son bir hafta!"
            r.daysLeft == 1 -> "Şafak 1 · ${Plates.cityFor(1)}" to "Yarın terhis!"
            else -> null
        }
        msg ?: return
        val key = "$today|${msg.first}"
        if (repo.lastMilestone() == key) return
        val n = NotificationCompat.Builder(context, CH_MILESTONE)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(RED)
            .setContentTitle(msg.first)
            .setContentText(msg.second)
            .setAutoCancel(true)
            .setContentIntent(openApp(context))
            .build()
        NotificationManagerCompat.from(context).notify(ID_MILESTONE, n)
        repo.setLastMilestone(key)
    }

    fun cancelAll(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_STATUS)
    }

    /** Gece yarısından birkaç saniye sonra güncelleme. Kesin alarm izni gerektirmez. */
    fun scheduleNextMidnight(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val next = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 5_000
        val pi = PendingIntent.getBroadcast(
            context, 0, Intent(context, SafakUpdateReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** Durum çubuğu için kalan gün sayısını yazan beyaz ikon. */
    private fun numberIcon(days: Int): IconCompat {
        val size = 96
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val text = days.toString()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = when (text.length) { 1 -> 80f; 2 -> 68f; else -> 48f }
        }
        val y = size / 2f - (paint.descent() + paint.ascent()) / 2f
        c.drawText(text, size / 2f, y, paint)
        return IconCompat.createWithBitmap(bmp)
    }
}
