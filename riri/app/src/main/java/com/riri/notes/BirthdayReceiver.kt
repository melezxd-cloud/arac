package com.riri.notes

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/** Alarm zamanı gelince bildirimi gösterir ve gelecek yılın alarmlarını kurar. */
class BirthdayReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Alarms.ensureChannel(ctx)
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, Alarms.CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(intent.getStringExtra("title") ?: "Riri's Notes")
            .setContentText(intent.getStringExtra("body") ?: "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(intent.getStringExtra("body") ?: ""))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(0xFFFF4FA3.toInt())
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        val allowed = Build.VERSION.SDK_INT < 33 ||
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (allowed) {
            NotificationManagerCompat.from(ctx).notify(intent.getIntExtra("code", 1), n)
        }
        Alarms.scheduleAll(ctx)
        RiriWidget.refreshAll(ctx)
    }
}
