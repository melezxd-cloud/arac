package com.denetim.arac;

import android.app.Activity;
import android.content.Context;
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

    /** Tür + bölüm, ör. "Hasar • Arka". */
    public static String turVeBolum(Db.Kayit k) {
        String s = k.tur == null ? "" : k.tur;
        if (k.bolum != null && !k.bolum.isEmpty() && !k.bolum.equals(BOLUMLER[0])) {
            s += "  •  " + k.bolum;
        }
        return s;
    }
}
