package com.safaksayar.pro.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private fun BroadcastReceiver.refreshAsync(context: Context) {
    val pending = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        try {
            SafakNotifier.refresh(context.applicationContext)
        } finally {
            pending.finish()
        }
    }
}

/** Gece yarısı alarmı: şafağı bir azaltır, bildirimi ve plakayı günceller. */
class SafakUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = refreshAsync(context)
}

/** Telefon açılınca, uygulama güncellenince veya saat/saat dilimi değişince yeniden kurar. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = refreshAsync(context)
}
