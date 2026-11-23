package com.vakit.namaz;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

/** Açık olan vakitler için telefonun alarm servisine kayıt düşer. */
public final class AlarmPlanlayici {

    private AlarmPlanlayici() {
    }

    private static PendingIntent niyet(Context c, int vakit) {
        Intent i = new Intent(c, AlarmAlici.class);
        i.setAction("com.vakit.namaz.VAKIT_" + vakit);
        i.putExtra(AlarmAlici.EK_VAKIT, vakit);
        return PendingIntent.getBroadcast(c, 100 + vakit, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Telefonun tam zamanlı alarm kurmasına izin verilmiş mi? */
    public static boolean tamAlarmIzni(Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        return am != null && am.canScheduleExactAlarms();
    }

    /** Bütün vakitlerin alarmını siler ve açık olanları yeniden kurar. */
    public static void yenidenKur(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        int il = Depo.il(c);
        boolean tam = tamAlarmIzni(c);

        for (int v = 0; v < Depo.VAKIT_SAYISI; v++) {
            PendingIntent pi = niyet(c, v);
            am.cancel(pi);

            if (!Depo.alarm(c, v)) continue;

            long zaman = siradakiZaman(c, il, v);
            if (zaman <= 0) continue;

            try {
                if (tam) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zaman, pi);
                } else {
                    // İzin yoksa yaklaşık zamanlı alarm kurulur; birkaç dakika sapabilir.
                    am.setWindow(AlarmManager.RTC_WAKEUP, zaman, 10 * 60 * 1000L, pi);
                }
            } catch (SecurityException e) {
                am.setWindow(AlarmManager.RTC_WAKEUP, zaman, 10 * 60 * 1000L, pi);
            }
        }
    }

    /**
     * Bir vaktin bundan sonraki ilk gerçekleşme anını bulur.
     * Önbellekte veri yoksa 0 döner.
     */
    static long siradakiZaman(Context c, int il, int vakit) {
        Calendar simdi = Depo.simdi();

        // Bugün ve sonraki 7 gün içinde ilk uygun anı ara
        for (int ileri = 0; ileri < 8; ileri++) {
            Calendar g = (Calendar) simdi.clone();
            g.add(Calendar.DAY_OF_MONTH, ileri);

            Depo.Gun veri = Depo.gun(c, il,
                    g.get(Calendar.YEAR), g.get(Calendar.MONTH) + 1, g.get(Calendar.DAY_OF_MONTH));
            if (veri == null) continue;

            Calendar hedef = Depo.gunBasi(
                    g.get(Calendar.YEAR), g.get(Calendar.MONTH) + 1, g.get(Calendar.DAY_OF_MONTH));
            hedef.add(Calendar.MINUTE, veri.dakika[vakit]);

            if (hedef.getTimeInMillis() > simdi.getTimeInMillis() + 30000L) {
                return hedef.getTimeInMillis();
            }
        }
        return 0;
    }
}
