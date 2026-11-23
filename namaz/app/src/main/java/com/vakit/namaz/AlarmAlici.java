package com.vakit.namaz;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

/** Vakit geldiğinde bildirim gösterir ve bir sonraki günün alarmını kurar. */
public class AlarmAlici extends BroadcastReceiver {

    public static final String EK_VAKIT = "vakit";
    private static final String KANAL = "vakit_bildirim";

    @Override
    public void onReceive(Context c, Intent intent) {
        int vakit = intent != null ? intent.getIntExtra(EK_VAKIT, -1) : -1;

        if (vakit >= 0 && vakit < Depo.VAKIT_SAYISI && Depo.alarm(c, vakit)) {
            bildirimGoster(c, vakit);
        }

        // Alarm tek seferliktir; bir sonraki vakit için yeniden kurulur.
        AlarmPlanlayici.yenidenKur(c);
    }

    private void bildirimGoster(Context c, int vakit) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        Uri ses = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel kanal = nm.getNotificationChannel(KANAL);
            if (kanal == null) {
                kanal = new NotificationChannel(KANAL, "Namaz vakti bildirimleri",
                        NotificationManager.IMPORTANCE_HIGH);
                kanal.setDescription("Namaz vakti girdiğinde bildirim gönderir.");
                kanal.enableVibration(true);
                kanal.setSound(ses, new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
                nm.createNotificationChannel(kanal);
            }
        }

        Intent ac = new Intent(c, MainActivity.class);
        ac.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 0, ac,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String ad = Depo.VAKIT_ADI[vakit];
        String il = Sehirler.ad(Depo.il(c));

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            b = new Notification.Builder(c, KANAL);
        } else {
            b = new Notification.Builder(c);
            b.setSound(ses);
            b.setDefaults(Notification.DEFAULT_VIBRATE);
            b.setPriority(Notification.PRIORITY_HIGH);
        }

        b.setSmallIcon(R.drawable.ic_bildirim)
                .setContentTitle(ad + " vakti girdi")
                .setContentText(il + " için " + ad + " vakti.")
                .setAutoCancel(true)
                .setContentIntent(pi);

        try {
            nm.notify(200 + vakit, b.build());
        } catch (SecurityException e) {
            // Bildirim izni verilmemiş; sessizce geçilir.
        }
    }
}
