package com.riri.notes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Telefon yeniden başlayınca ya da uygulama güncellenince alarmlar yeniden kurulur. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Alarms.scheduleAll(ctx)
        RiriWidget.refreshAll(ctx)
    }
}
