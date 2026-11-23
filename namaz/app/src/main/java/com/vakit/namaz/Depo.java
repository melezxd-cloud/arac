package com.vakit.namaz;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Vakit verisinin tek kaynağı.
 *
 * Veriler AlAdhan API'sinden Diyanet hesaplama yöntemiyle (method 13) aylık
 * olarak çekilir ve telefonun kendi hafızasına kaydedilir. Böylece ay içinde
 * internet olmasa da vakitler görünmeye devam eder ve alarmlar kurulabilir.
 */
public final class Depo {

    public static final String[] VAKIT_ADI = { "İmsak", "Güneş", "Öğle", "İkindi", "Akşam", "Yatsı" };
    private static final String[] API_ANAHTAR = { "Imsak", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha" };
    public static final int VAKIT_SAYISI = 6;

    private static final String AYAR = "vakit_ayar";
    private static final String A_IL = "il";
    private static final String A_ALARM = "alarm_";
    private static final String A_SORULDU = "bildirim_soruldu";

    public static final TimeZone TR = TimeZone.getTimeZone("Europe/Istanbul");
    private static final Locale TR_DIL = Locale.forLanguageTag("tr-TR");

    private Depo() {
    }

    /** Bir günün altı vakti ve hicri tarihi. */
    public static class Gun {
        /** Gece yarısından itibaren dakika cinsinden altı vakit. */
        public final int[] dakika = new int[VAKIT_SAYISI];
        public String hicri = "";
    }

    // ---------------- Ayarlar ----------------

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(AYAR, Context.MODE_PRIVATE);
    }

    public static int il(Context c) {
        return p(c).getInt(A_IL, Sehirler.VARSAYILAN);
    }

    public static void ilAyarla(Context c, int il) {
        p(c).edit().putInt(A_IL, il).apply();
    }

    /** Varsayılan: Güneş hariç bütün vakitlerde alarm açık. */
    public static boolean alarm(Context c, int vakit) {
        return p(c).getBoolean(A_ALARM + vakit, vakit != 1);
    }

    public static void alarmAyarla(Context c, int vakit, boolean acik) {
        p(c).edit().putBoolean(A_ALARM + vakit, acik).apply();
    }

    public static boolean bildirimSoruldu(Context c) {
        return p(c).getBoolean(A_SORULDU, false);
    }

    public static void bildirimSorulduIsaretle(Context c) {
        p(c).edit().putBoolean(A_SORULDU, true).apply();
    }

    // ---------------- Takvim yardımcıları ----------------

    public static Calendar simdi() {
        return Calendar.getInstance(TR, TR_DIL);
    }

    /** Verilen gün için, o günün vakitlerini taşıyan Calendar nesnesi üretir. */
    public static Calendar gunBasi(int yil, int ay, int gun) {
        Calendar c = Calendar.getInstance(TR, TR_DIL);
        c.clear();
        c.set(yil, ay - 1, gun, 0, 0, 0);
        return c;
    }

    // ---------------- Önbellek ----------------

    private static File dosya(Context c, int il, int yil, int ay) {
        return new File(c.getFilesDir(), "v_" + il + "_" + yil + "_" + ay + ".json");
    }

    public static boolean onbellekVar(Context c, int il, int yil, int ay) {
        File f = dosya(c, il, yil, ay);
        return f.exists() && f.length() > 100;
    }

    /**
     * Son okunan ay bellekte tutulur. Geri sayım saniyede bir çalıştığı ve
     * alarmlar kurulurken günlere tek tek bakıldığı için, her seferinde
     * dosyadan okumak arayüzü yavaşlatırdı.
     */
    private static String bellekAnahtar = "";
    private static JSONArray bellekVeri;

    private static synchronized JSONArray ay(Context c, int il, int yil, int ayNo) {
        String anahtar = il + "_" + yil + "_" + ayNo;
        if (anahtar.equals(bellekAnahtar) && bellekVeri != null) return bellekVeri;

        JSONArray arr = ayOku(c, il, yil, ayNo);
        if (arr != null) {
            bellekAnahtar = anahtar;
            bellekVeri = arr;
        }
        return arr;
    }

    /** İndirme sonrası bellekteki eski veriyi düşürür. */
    private static synchronized void bellegiTemizle() {
        bellekAnahtar = "";
        bellekVeri = null;
    }

    private static JSONArray ayOku(Context c, int il, int yil, int ayNo) {
        File f = dosya(c, il, yil, ayNo);
        if (!f.exists()) return null;
        try (InputStream is = new java.io.FileInputStream(f)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] t = new byte[8192];
            int n;
            while ((n = is.read(t)) != -1) bos.write(t, 0, n);
            return new JSONArray(new String(bos.toByteArray(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Önbellekten bir günün vakitlerini okur. Veri yoksa null döner.
     * gun 1'den başlar, ay 1-12 arasıdır.
     */
    public static Gun gun(Context c, int il, int yil, int ayNo, int gunNo) {
        JSONArray arr = ay(c, il, yil, ayNo);
        if (arr == null || gunNo < 1 || gunNo > arr.length()) return null;
        try {
            JSONObject o = arr.getJSONObject(gunNo - 1);
            Gun g = new Gun();
            for (int i = 0; i < VAKIT_SAYISI; i++) {
                g.dakika[i] = o.getInt(API_ANAHTAR[i]);
            }
            g.hicri = o.optString("hicri", "");
            return g;
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------- İnternetten çekme ----------------

    /**
     * Bir ayın vakitlerini indirip önbelleğe yazar. Arka plan iş parçacığından
     * çağrılmalıdır. Başarılıysa true döner.
     */
    public static boolean indir(Context c, int il, int yil, int ayNo) {
        HttpURLConnection baglanti = null;
        try {
            String adres = String.format(Locale.US,
                    "https://api.aladhan.com/v1/calendar/%d/%d?latitude=%.4f&longitude=%.4f"
                            + "&method=13&timezonestring=Europe%%2FIstanbul",
                    yil, ayNo, Sehirler.enlem(il), Sehirler.boylam(il));

            URL url = new URL(adres);
            baglanti = (HttpURLConnection) url.openConnection();
            baglanti.setConnectTimeout(15000);
            baglanti.setReadTimeout(20000);
            baglanti.setRequestProperty("Accept", "application/json");

            if (baglanti.getResponseCode() != 200) return false;

            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(baglanti.getInputStream(), StandardCharsets.UTF_8))) {
                String satir;
                while ((satir = r.readLine()) != null) sb.append(satir);
            }

            JSONObject kok = new JSONObject(sb.toString());
            JSONArray gunler = kok.getJSONArray("data");

            // API cevabını sadeleştirip kaydediyoruz: her gün için 6 vakit (dakika) + hicri tarih
            JSONArray sade = new JSONArray();
            for (int i = 0; i < gunler.length(); i++) {
                JSONObject g = gunler.getJSONObject(i);
                JSONObject timings = g.getJSONObject("timings");
                JSONObject yeni = new JSONObject();
                for (int v = 0; v < VAKIT_SAYISI; v++) {
                    yeni.put(API_ANAHTAR[v], dakikayaCevir(timings.getString(API_ANAHTAR[v])));
                }
                JSONObject hicri = g.getJSONObject("date").optJSONObject("hijri");
                if (hicri != null) {
                    String ayAdi = hicriAy(hicri.optJSONObject("month"));
                    if (ayAdi.isEmpty()) {
                        yeni.put("hicri", hicri.optString("date", ""));
                    } else {
                        yeni.put("hicri", hicri.optString("day", "") + " " + ayAdi
                                + " " + hicri.optString("year", ""));
                    }
                }
                sade.put(yeni);
            }

            if (sade.length() < 28) return false;

            File f = dosya(c, il, yil, ayNo);
            try (FileOutputStream os = new FileOutputStream(f)) {
                os.write(sade.toString().getBytes(StandardCharsets.UTF_8));
            }
            bellegiTemizle();
            return true;

        } catch (Exception e) {
            return false;
        } finally {
            if (baglanti != null) baglanti.disconnect();
        }
    }

    /** Hicri ay adını Türkçeye çevirir. */
    private static String hicriAy(JSONObject ay) {
        if (ay == null) return "";
        int n = ay.optInt("number", 0);
        String[] adlar = {
                "", "Muharrem", "Safer", "Rebîülevvel", "Rebîülâhir",
                "Cemâziyelevvel", "Cemâziyelâhir", "Receb", "Şaban",
                "Ramazan", "Şevval", "Zilkade", "Zilhicce"
        };
        return (n >= 1 && n <= 12) ? adlar[n] : ay.optString("en", "");
    }

    /** "05:46 (+03)" biçimindeki saati gece yarısından itibaren dakikaya çevirir. */
    static int dakikayaCevir(String ham) {
        if (ham == null) return 0;
        String s = ham.trim();
        int bosluk = s.indexOf(' ');
        if (bosluk > 0) s = s.substring(0, bosluk);
        String[] parca = s.split(":");
        if (parca.length < 2) return 0;
        try {
            int saat = Integer.parseInt(parca[0].trim());
            int dk = Integer.parseInt(parca[1].trim());
            return saat * 60 + dk;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String saatYaz(int dakika) {
        return String.format(Locale.US, "%02d:%02d", (dakika / 60) % 24, dakika % 60);
    }
}
