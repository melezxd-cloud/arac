package com.denetim.arac;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Ortak küçük yardımcılar: tarih biçimi, mesaj, klavye. */
public final class Yardim {

    private static final Locale TR = Locale.forLanguageTag("tr-TR");

    /** Kayıt türleri (listede bu sırayla görünür). */
    public static final String[] TURLER = {
            "Hasar",
            "Arıza",
            "Eksik parça",
            "Temizlik",
            "Güvenlik",
            "Kontrol – sorun yok",
            "Diğer"
    };

    /** Aracın bölümleri. */
    public static final String[] BOLUMLER = {
            "Belirtilmedi",
            "Ön",
            "Arka",
            "Sağ yan",
            "Sol yan",
            "Tavan",
            "Kapılar",
            "Camlar",
            "İç kabin / koltuklar",
            "Sürücü mahalli",
            "Motor",
            "Lastik / jant",
            "Elektrik / aydınlatma",
            "Klima / ısıtma"
    };

    public static final String KONTROL = "Kontrol – sorun yok";

    private Yardim() {
    }

    public static String tarihSaat(long ms) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm", TR).format(new Date(ms));
    }

    public static String tarih(long ms) {
        return new SimpleDateFormat("dd.MM.yyyy", TR).format(new Date(ms));
    }

    public static void mesaj(Context c, String yazi) {
        Toast.makeText(c, yazi, Toast.LENGTH_SHORT).show();
    }

    public static void klavyeyiKapat(Activity a) {
        View v = a.getCurrentFocus();
        if (v == null) v = a.getWindow().getDecorView();
        InputMethodManager imm = (InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    public static String saat(long ms) {
        return new SimpleDateFormat("HH:mm", TR).format(new Date(ms));
    }

    // ---------------- Hızlı not ayarları ----------------

    private static final String AYAR = "denetim_ayar";

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(AYAR, Context.MODE_PRIVATE);
    }

    /** Tesis adı; hazır metnin başına gelir. */
    public static String tesis(Context c) {
        return p(c).getString("tesis", "Başakşehir Sys");
    }

    public static void tesisAyarla(Context c, String ad) {
        p(c).edit().putString("tesis", ad).apply();
    }

    /** En son seçilen peron hatırlanır. */
    public static int peron(Context c) {
        return p(c).getInt("peron", 0);
    }

    public static void peronAyarla(Context c, int i) {
        p(c).edit().putInt("peron", i).apply();
    }

    public static boolean ciftli(Context c) {
        return p(c).getBoolean("ciftli", false);
    }

    public static void ciftliAyarla(Context c, boolean v) {
        p(c).edit().putBoolean("ciftli", v).apply();
    }

    /** Açıkken metnin sonuna "olduğu tespit edildi." eklenir. */
    public static boolean otomatikCumle(Context c) {
        return p(c).getBoolean("oto_cumle", true);
    }

    public static void otomatikCumleAyarla(Context c, boolean v) {
        p(c).edit().putBoolean("oto_cumle", v).apply();
    }

    /** Metni panoya kopyalar. */
    public static void panoyaKopyala(Context c, String metin) {
        ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) {
            mesaj(c, "Pano açılamadı.");
            return;
        }
        cm.setPrimaryClip(ClipData.newPlainText("Hızlı not", metin));
        // Android 13 ve üstü kendi "kopyalandı" bildirimini gösterir, iki kez çıkmasın.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            mesaj(c, "Metin kopyalandı");
        }
    }

    /** Tür + bölüm, ör. "Hasar • Arka". */
    public static String turVeBolum(Db.Kayit k) {
        String s = k.tur == null ? "" : k.tur;
        if (k.bolum != null && !k.bolum.isEmpty() && !k.bolum.equals(BOLUMLER[0])) {
            s += "  •  " + k.bolum;
        }
        return s;
    }
}
