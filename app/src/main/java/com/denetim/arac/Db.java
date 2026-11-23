package com.denetim.arac;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Telefon hafızasındaki SQLite veritabanı.
 * Tablolar:
 *   arac  : kayıtlı araç kodları
 *   kayit : her araca ait denetim / hasar notları
 */
public class Db extends SQLiteOpenHelper {

    private static final String DOSYA = "denetim.db";
    private static final int SURUM = 1;
    private static final Locale TR = Locale.forLanguageTag("tr-TR");

    private static Db ornek;

    public static synchronized Db al(Context c) {
        if (ornek == null) ornek = new Db(c.getApplicationContext());
        return ornek;
    }

    private Db(Context c) {
        super(c, DOSYA, null, SURUM);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE arac (" +
                "kod TEXT PRIMARY KEY, " +
                "olusturma INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE kayit (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "kod TEXT NOT NULL, " +
                "tarih INTEGER NOT NULL, " +
                "tur TEXT NOT NULL, " +
                "bolum TEXT, " +
                "notlar TEXT, " +
                "giderildi INTEGER NOT NULL DEFAULT 0, " +
                "giderilme INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX idx_kayit_kod ON kayit(kod)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int eski, int yeni) {
        // İleride tablo değişikliği olursa burada yapılır.
    }

    // ---------------- Modeller ----------------

    public static class Arac {
        public String kod;
        public long olusturma;
        public int toplam;
        public int acik;
        public long sonTarih;
    }

    public static class Kayit {
        public long id;
        public String kod;
        public long tarih;
        public String tur;
        public String bolum;
        public String notlar;
        public boolean giderildi;
        public long giderilme;
    }

    /** Araç kodunu standart hale getirir: boşlukları siler, büyük harfe çevirir (k 1565 -> K1565). */
    public static String duzenle(String kod) {
        if (kod == null) return "";
        return kod.replaceAll("\\s+", "").toUpperCase(TR);
    }

    // ---------------- Araçlar ----------------

    public boolean aracVar(String kod) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT 1 FROM arac WHERE kod = ?", new String[]{kod})) {
            return c.moveToFirst();
        }
    }

    public void aracEkle(String kod) {
        ContentValues v = new ContentValues();
        v.put("kod", kod);
        v.put("olusturma", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("arac", null, v, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private static final String ARAC_SORGU =
            "SELECT a.kod, a.olusturma, COUNT(k.id), " +
            "SUM(CASE WHEN k.giderildi = 0 THEN 1 ELSE 0 END), MAX(k.tarih) " +
            "FROM arac a LEFT JOIN kayit k ON k.kod = a.kod ";

    /** Araç listesi; en son işlem yapılan araç en üstte. filtre boşsa hepsi gelir. */
    public List<Arac> araclar(String filtre) {
        List<Arac> liste = new ArrayList<>();
        String sql = ARAC_SORGU + "WHERE a.kod LIKE ? GROUP BY a.kod " +
                "ORDER BY COALESCE(MAX(k.tarih), a.olusturma) DESC";
        String desen = "%" + (filtre == null ? "" : filtre) + "%";
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{desen})) {
            while (c.moveToNext()) liste.add(aracOku(c));
        }
        return liste;
    }

    /** Tek bir aracın özet bilgisi; araç yoksa null. */
    public Arac arac(String kod) {
        String sql = ARAC_SORGU + "WHERE a.kod = ? GROUP BY a.kod";
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{kod})) {
            return c.moveToFirst() ? aracOku(c) : null;
        }
    }

    private static Arac aracOku(Cursor c) {
        Arac a = new Arac();
        a.kod = c.getString(0);
        a.olusturma = c.getLong(1);
        a.toplam = c.getInt(2);
        a.acik = c.isNull(3) ? 0 : c.getInt(3);
        a.sonTarih = c.isNull(4) ? 0 : c.getLong(4);
        return a;
    }

    public void aracSil(String kod) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("kayit", "kod = ?", new String[]{kod});
            db.delete("arac", "kod = ?", new String[]{kod});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // ---------------- Kayıtlar ----------------

    public static final int FILTRE_TUMU = 0;
    public static final int FILTRE_ACIK = 1;
    public static final int FILTRE_GIDERILEN = 2;

    /** Bir aracın kayıtları; en yeni en üstte. */
    public List<Kayit> kayitlar(String kod, int filtre) {
        String sql = "SELECT * FROM kayit WHERE kod = ?";
        if (filtre == FILTRE_ACIK) sql += " AND giderildi = 0";
        else if (filtre == FILTRE_GIDERILEN) sql += " AND giderildi = 1";
        sql += " ORDER BY tarih DESC, id DESC";
        List<Kayit> liste = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{kod})) {
            while (c.moveToNext()) liste.add(kayitOku(c));
        }
        return liste;
    }

    public Kayit kayit(long id) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM kayit WHERE id = ?", new String[]{String.valueOf(id)})) {
            return c.moveToFirst() ? kayitOku(c) : null;
        }
    }

    private static Kayit kayitOku(Cursor c) {
        Kayit k = new Kayit();
        k.id = c.getLong(c.getColumnIndexOrThrow("id"));
        k.kod = c.getString(c.getColumnIndexOrThrow("kod"));
        k.tarih = c.getLong(c.getColumnIndexOrThrow("tarih"));
        k.tur = c.getString(c.getColumnIndexOrThrow("tur"));
        k.bolum = c.getString(c.getColumnIndexOrThrow("bolum"));
        k.notlar = c.getString(c.getColumnIndexOrThrow("notlar"));
        k.giderildi = c.getInt(c.getColumnIndexOrThrow("giderildi")) == 1;
        k.giderilme = c.getLong(c.getColumnIndexOrThrow("giderilme"));
        return k;
    }

    /** Yeni kaydı ekler (id = 0) veya mevcut kaydı günceller. */
    public long kaydet(Kayit k) {
        aracEkle(k.kod); // araç yoksa oluşturulur, varsa dokunulmaz
        ContentValues v = new ContentValues();
        v.put("kod", k.kod);
        v.put("tarih", k.tarih);
        v.put("tur", k.tur);
        v.put("bolum", k.bolum);
        v.put("notlar", k.notlar);
        v.put("giderildi", k.giderildi ? 1 : 0);
        v.put("giderilme", k.giderilme);
        SQLiteDatabase db = getWritableDatabase();
        if (k.id == 0) {
            k.id = db.insert("kayit", null, v);
        } else {
            db.update("kayit", v, "id = ?", new String[]{String.valueOf(k.id)});
        }
        return k.id;
    }

    public void kayitSil(long id) {
        getWritableDatabase().delete("kayit", "id = ?", new String[]{String.valueOf(id)});
    }

    // ---------------- Yedekleme ----------------

    /** Tüm verileri tek bir JSON nesnesine çevirir. */
    public JSONObject yedek() throws JSONException {
        JSONObject kok = new JSONObject();
        kok.put("uygulama", "AracDenetim");
        kok.put("surum", SURUM);
        kok.put("tarih", System.currentTimeMillis());

        JSONArray araclar = new JSONArray();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT kod, olusturma FROM arac", null)) {
            while (c.moveToNext()) {
                JSONObject o = new JSONObject();
                o.put("kod", c.getString(0));
                o.put("olusturma", c.getLong(1));
                araclar.put(o);
            }
        }
        kok.put("araclar", araclar);

        JSONArray kayitlar = new JSONArray();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT * FROM kayit ORDER BY id", null)) {
            while (c.moveToNext()) {
                Kayit k = kayitOku(c);
                JSONObject o = new JSONObject();
                o.put("kod", k.kod);
                o.put("tarih", k.tarih);
                o.put("tur", k.tur);
                o.put("bolum", k.bolum == null ? "" : k.bolum);
                o.put("notlar", k.notlar == null ? "" : k.notlar);
                o.put("giderildi", k.giderildi);
                o.put("giderilme", k.giderilme);
                kayitlar.put(o);
            }
        }
        kok.put("kayitlar", kayitlar);
        return kok;
    }

    /** Telefondaki verileri silip yedekteki verileri yükler. */
    public void yedektenYukle(JSONObject kok) throws JSONException {
        JSONArray araclar = kok.getJSONArray("araclar");
        JSONArray kayitlar = kok.getJSONArray("kayitlar");
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("kayit", null, null);
            db.delete("arac", null, null);
            for (int i = 0; i < araclar.length(); i++) {
                JSONObject o = araclar.getJSONObject(i);
                ContentValues v = new ContentValues();
                v.put("kod", o.getString("kod"));
                v.put("olusturma", o.optLong("olusturma", System.currentTimeMillis()));
                db.insertWithOnConflict("arac", null, v, SQLiteDatabase.CONFLICT_IGNORE);
            }
            for (int i = 0; i < kayitlar.length(); i++) {
                JSONObject o = kayitlar.getJSONObject(i);
                String kod = o.getString("kod");
                ContentValues a = new ContentValues();
                a.put("kod", kod);
                a.put("olusturma", o.optLong("tarih", System.currentTimeMillis()));
                db.insertWithOnConflict("arac", null, a, SQLiteDatabase.CONFLICT_IGNORE);

                ContentValues v = new ContentValues();
                v.put("kod", kod);
                v.put("tarih", o.getLong("tarih"));
                v.put("tur", o.optString("tur", "Diğer"));
                v.put("bolum", o.optString("bolum", ""));
                v.put("notlar", o.optString("notlar", ""));
                v.put("giderildi", o.optBoolean("giderildi", false) ? 1 : 0);
                v.put("giderilme", o.optLong("giderilme", 0));
                db.insert("kayit", null, v);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
