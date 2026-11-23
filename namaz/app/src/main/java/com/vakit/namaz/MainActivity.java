package com.vakit.namaz;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Ana ekran: seçili ilin bugünkü vakitleri, geri sayım ve alarm düğmeleri. */
public class MainActivity extends Activity {

    private static final int ISTEK_IL = 1;
    private static final int ISTEK_BILDIRIM = 2;
    private static final Locale TR_DIL = Locale.forLanguageTag("tr-TR");

    private final Handler saatci = new Handler(Looper.getMainLooper());
    private final ExecutorService havuz = Executors.newSingleThreadExecutor();

    private TextView tvIl, tvTarih, tvHicri, tvSonrakiBaslik, tvSayac, tvSayacAlt, tvUyari;
    private LinearLayout vakitListesi;
    private AdView reklam;

    private final View[] satir = new View[Depo.VAKIT_SAYISI];
    private final TextView[] satirAd = new TextView[Depo.VAKIT_SAYISI];
    private final TextView[] satirSaat = new TextView[Depo.VAKIT_SAYISI];
    private final ImageView[] satirZil = new ImageView[Depo.VAKIT_SAYISI];

    private Depo.Gun bugun;
    private boolean indiriliyor;
    /** En son vurgulanan vakit; her saniye yeniden çizmemek için tutulur. */
    private int sonAktif = -2;

    private final Runnable tik = new Runnable() {
        @Override
        public void run() {
            sayaciGuncelle();
            saatci.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvIl = findViewById(R.id.tvIl);
        tvTarih = findViewById(R.id.tvTarih);
        tvHicri = findViewById(R.id.tvHicri);
        tvSonrakiBaslik = findViewById(R.id.tvSonrakiBaslik);
        tvSayac = findViewById(R.id.tvSayac);
        tvSayacAlt = findViewById(R.id.tvSayacAlt);
        tvUyari = findViewById(R.id.tvUyari);
        vakitListesi = findViewById(R.id.vakitListesi);
        reklam = findViewById(R.id.reklam);

        satirlariKur();

        findViewById(R.id.btnIlSec).setOnClickListener(v -> {
            Intent i = new Intent(this, IlActivity.class);
            startActivityForResult(i, ISTEK_IL);
        });

        reklamiKur();
        bildirimIzniIste();
    }

    /** Altı vakit satırını bir kez oluşturur. */
    private void satirlariKur() {
        LayoutInflater li = LayoutInflater.from(this);
        for (int v = 0; v < Depo.VAKIT_SAYISI; v++) {
            final int vakit = v;
            View s = li.inflate(R.layout.item_vakit, vakitListesi, false);
            satirAd[v] = s.findViewById(R.id.tvAd);
            satirSaat[v] = s.findViewById(R.id.tvSaat);
            satirZil[v] = s.findViewById(R.id.btnZil);

            satirAd[v].setText(Depo.VAKIT_ADI[v]);
            satirZil[v].setOnClickListener(x -> alarmCevir(vakit));

            satir[v] = s;
            vakitListesi.addView(s);
        }
        zilleriGuncelle();
    }

    private void reklamiKur() {
        MobileAds.initialize(this, durum -> { });
        reklam.setAdUnitId(getString(R.string.admob_banner_id));
        reklam.setAdSize(bannerBoyutu());
        reklam.loadAd(new AdRequest.Builder().build());
    }

    /** Ekran genişliğine uyan banner boyutu. */
    private AdSize bannerBoyutu() {
        DisplayMetrics m = getResources().getDisplayMetrics();
        int genislikDp = (int) (m.widthPixels / m.density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, genislikDp);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ekraniTazele();
        saatci.post(tik);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saatci.removeCallbacks(tik);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        havuz.shutdownNow();
        if (reklam != null) reklam.destroy();
    }

    @Override
    protected void onActivityResult(int istek, int sonuc, Intent veri) {
        super.onActivityResult(istek, sonuc, veri);
        if (istek == ISTEK_IL && sonuc == RESULT_OK) {
            ekraniTazele();
        }
    }

    @Override
    public void onRequestPermissionsResult(int istek, String[] izinler, int[] sonuclar) {
        super.onRequestPermissionsResult(istek, izinler, sonuclar);
        if (istek == ISTEK_BILDIRIM) uyariyiGuncelle();
    }

    // ---------------- Veri ----------------

    private void ekraniTazele() {
        sonAktif = -2;   // il veya gün değişmiş olabilir, vurguyu yeniden çiz
        int il = Depo.il(this);
        tvIl.setText(Sehirler.ad(il));

        Calendar simdi = Depo.simdi();
        SimpleDateFormat bicim = new SimpleDateFormat("d MMMM yyyy, EEEE", TR_DIL);
        bicim.setTimeZone(Depo.TR);
        tvTarih.setText(bicim.format(simdi.getTime()));

        bugun = Depo.gun(this, il,
                simdi.get(Calendar.YEAR), simdi.get(Calendar.MONTH) + 1,
                simdi.get(Calendar.DAY_OF_MONTH));

        if (bugun != null) {
            tvHicri.setText(bugun.hicri);
            for (int v = 0; v < Depo.VAKIT_SAYISI; v++) {
                satirSaat[v].setText(Depo.saatYaz(bugun.dakika[v]));
            }
        } else {
            tvHicri.setText("");
            for (int v = 0; v < Depo.VAKIT_SAYISI; v++) {
                satirSaat[v].setText("--:--");
            }
        }

        zilleriGuncelle();
        sayaciGuncelle();
        uyariyiGuncelle();
        gerekirseIndir();
    }

    /** Önbellekte bu ayın verisi yoksa indirir. Ay sonunda gelecek ayı da alır. */
    private void gerekirseIndir() {
        if (indiriliyor) return;

        final int il = Depo.il(this);
        Calendar simdi = Depo.simdi();
        final int yil = simdi.get(Calendar.YEAR);
        final int ay = simdi.get(Calendar.MONTH) + 1;

        boolean buAyLazim = !Depo.onbellekVar(this, il, yil, ay);

        Calendar sonra = (Calendar) simdi.clone();
        sonra.add(Calendar.DAY_OF_MONTH, 7);
        final int yil2 = sonra.get(Calendar.YEAR);
        final int ay2 = sonra.get(Calendar.MONTH) + 1;
        boolean sonrakiLazim = (ay2 != ay) && !Depo.onbellekVar(this, il, yil2, ay2);

        if (!buAyLazim && !sonrakiLazim) {
            AlarmPlanlayici.yenidenKur(this);
            return;
        }

        final boolean ilkAy = buAyLazim;
        final boolean ikinciAy = sonrakiLazim;
        indiriliyor = true;
        if (bugun == null) {
            tvSayac.setText("...");
            tvSayacAlt.setText("Vakitler indiriliyor");
        }

        havuz.execute(() -> {
            boolean tamam = true;
            if (ilkAy) tamam = Depo.indir(MainActivity.this, il, yil, ay);
            if (ikinciAy) Depo.indir(MainActivity.this, il, yil2, ay2);
            final boolean basarili = tamam;

            saatci.post(() -> {
                indiriliyor = false;
                if (isFinishing() || isDestroyed()) return;
                if (basarili) {
                    AlarmPlanlayici.yenidenKur(MainActivity.this);
                    ekraniTazele();
                } else {
                    uyariyiGuncelle();
                    if (bugun == null) {
                        tvSayac.setText("--:--:--");
                        tvSayacAlt.setText("Vakitler alınamadı");
                    }
                }
            });
        });
    }

    // ---------------- Geri sayım ----------------

    private void sayaciGuncelle() {
        if (bugun == null) return;

        Calendar simdi = Depo.simdi();
        int gecenDakika = simdi.get(Calendar.HOUR_OF_DAY) * 60 + simdi.get(Calendar.MINUTE);
        int saniye = simdi.get(Calendar.SECOND);

        int siradaki = -1;
        for (int v = 0; v < Depo.VAKIT_SAYISI; v++) {
            if (bugun.dakika[v] > gecenDakika
                    || (bugun.dakika[v] == gecenDakika && saniye == 0)) {
                siradaki = v;
                break;
            }
        }

        // Şu an hangi vakit içindeyiz: sıradakinden bir önceki.
        // Gece yarısı ile imsak arası da hâlâ yatsı vaktidir.
        int aktif = (siradaki <= 0) ? Depo.VAKIT_SAYISI - 1 : siradaki - 1;
        satirlariIsaretle(aktif);

        long kalanSaniye;
        String ad;

        if (siradaki == -1) {
            // Yatsı geçti; sıradaki yarının imsakı
            Calendar yarin = (Calendar) simdi.clone();
            yarin.add(Calendar.DAY_OF_MONTH, 1);
            Depo.Gun g = Depo.gun(this, Depo.il(this),
                    yarin.get(Calendar.YEAR), yarin.get(Calendar.MONTH) + 1,
                    yarin.get(Calendar.DAY_OF_MONTH));
            int imsak = (g != null) ? g.dakika[0] : bugun.dakika[0];
            kalanSaniye = (long) (24 * 60 - gecenDakika + imsak) * 60 - saniye;
            ad = Depo.VAKIT_ADI[0];
        } else {
            kalanSaniye = (long) (bugun.dakika[siradaki] - gecenDakika) * 60 - saniye;
            ad = Depo.VAKIT_ADI[siradaki];
        }

        if (kalanSaniye < 0) kalanSaniye = 0;

        long s = kalanSaniye / 3600;
        long d = (kalanSaniye % 3600) / 60;
        long sn = kalanSaniye % 60;

        tvSonrakiBaslik.setText("SONRAKİ VAKİT · " + ad.toUpperCase(TR_DIL));
        tvSayac.setText(String.format(Locale.US, "%02d:%02d:%02d", s, d, sn));
        tvSayacAlt.setText(ad + " vaktine kalan süre");
    }

    /** O an içinde bulunulan vakti altın rengiyle öne çıkarır. */
    private void satirlariIsaretle(int aktif) {
        if (aktif == sonAktif) return;
        sonAktif = aktif;
        for (int v = 0; v < Depo.VAKIT_SAYISI; v++) {
            boolean su = (v == aktif);
            satir[v].setBackgroundResource(su ? R.drawable.kart_aktif : R.drawable.kart);
            int renk = su ? getColor(R.color.koyu_metin) : getColor(R.color.krem_yumusak);
            int renkSaat = su ? getColor(R.color.koyu_metin) : getColor(R.color.krem);
            satirAd[v].setTextColor(renk);
            satirSaat[v].setTextColor(renkSaat);
            satirAd[v].setTypeface(null, su ? android.graphics.Typeface.BOLD
                    : android.graphics.Typeface.NORMAL);
        }
    }

    // ---------------- Alarmlar ----------------

    private void alarmCevir(int vakit) {
        boolean yeni = !Depo.alarm(this, vakit);
        Depo.alarmAyarla(this, vakit, yeni);
        zilleriGuncelle();
        AlarmPlanlayici.yenidenKur(this);
        Toast.makeText(this,
                Depo.VAKIT_ADI[vakit] + (yeni ? " alarmı açıldı" : " alarmı kapatıldı"),
                Toast.LENGTH_SHORT).show();
        uyariyiGuncelle();
    }

    private void zilleriGuncelle() {
        for (int v = 0; v < Depo.VAKIT_SAYISI; v++) {
            boolean acik = Depo.alarm(this, v);
            satirZil[v].setImageResource(acik ? R.drawable.ic_zil : R.drawable.ic_zil_kapali);
            satirZil[v].setContentDescription(
                    Depo.VAKIT_ADI[v] + (acik ? " alarmını kapat" : " alarmını aç"));
        }
    }

    private void bildirimIzniIste() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (Depo.bildirimSoruldu(this)) return;
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) return;
        Depo.bildirimSorulduIsaretle(this);
        requestPermissions(new String[]{ Manifest.permission.POST_NOTIFICATIONS }, ISTEK_BILDIRIM);
    }

    /** Eksik izin veya veri varsa üstte uyarı satırı gösterir. */
    private void uyariyiGuncelle() {
        boolean bildirimKapali = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED;

        boolean alarmIzniYok = !AlarmPlanlayici.tamAlarmIzni(this);
        boolean alarmVar = false;
        for (int v = 0; v < Depo.VAKIT_SAYISI; v++) {
            if (Depo.alarm(this, v)) { alarmVar = true; break; }
        }

        if (bildirimKapali && alarmVar) {
            uyariGoster("Bildirim izni kapalı. Vakit alarmlarının çalışması için dokunup izin verin.",
                    v -> ayarlariAc());
        } else if (alarmIzniYok && alarmVar) {
            uyariGoster("Alarmların tam zamanında çalması için izin gerekiyor. Dokunup açın.",
                    v -> tamAlarmAyariniAc());
        } else if (bugun == null && !indiriliyor) {
            uyariGoster("Vakitler indirilemedi. İnternete bağlanıp dokunarak tekrar deneyin.",
                    v -> gerekirseIndir());
        } else {
            tvUyari.setVisibility(View.GONE);
        }
    }

    private void uyariGoster(String yazi, View.OnClickListener tiklama) {
        tvUyari.setText(yazi);
        tvUyari.setVisibility(View.VISIBLE);
        tvUyari.setOnClickListener(tiklama);
    }

    private void ayarlariAc() {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.setData(Uri.fromParts("package", getPackageName(), null));
        try {
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Ayarlar açılamadı.", Toast.LENGTH_SHORT).show();
        }
    }

    private void tamAlarmAyariniAc() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        try {
            Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            i.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(i);
        } catch (Exception e) {
            ayarlariAc();
        }
    }
}
