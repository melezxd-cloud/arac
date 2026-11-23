package com.vakit.namaz;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Telefon yeniden başladığında veya uygulama güncellendiğinde alarmları yeniden kurar. */
public class AcilisAlici extends BroadcastReceiver {

    @Override
    public void onReceive(Context c, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String a = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(a)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) {
            AlarmPlanlayici.yenidenKur(c);
        }
    }
}
