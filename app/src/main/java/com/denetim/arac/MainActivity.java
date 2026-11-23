package com.denetim.arac;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Giriş ekranı: araç kodu yazılır, kayıtlı araçlar listelenir. */
public class MainActivity extends Activity {

    private static final int ISTEK_YEDEK_AL = 1;
    private static final int ISTEK_YEDEK_YUKLE = 2;

    private Db db;
    private EditText etKod;
    private ListView lvAraclar;
    private TextView tvBos;
    private TextView tvListeBaslik;
    private final AracAdapter adapter = new AracAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = Db.al(this);

        etKod = findViewById(R.id.etKod);
        lvAraclar = findViewById(R.id.lvAraclar);
        tvBos = findViewById(R.id.tvBos);
        tvListeBaslik = findViewById(R.id.tvListeBaslik);

        lvAraclar.setAdapter(adapter);
        lvAraclar.setOnItemClickListener((parent, view, pos, id) -> aracAc(adapter.getItem(pos).kod));

        findViewById(R.id.btnGiris).setOnClickListener(v -> giris());
        findViewById(R.id.btnMenu).setOnClickListener(this::menuGoster);

        etKod.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE || enter) {
                giris();
                return true;
            }
            return false;
        });

        // Yazdıkça listeyi süz: "K15" yazınca K15 ile eşleşen araçlar görünür.
        etKod.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { listeyiYenile(); }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        listeyiYenile();
    }

    /** GİRİŞ: araç kayıtlıysa açar, değilse yeni araç olarak kaydetmeyi sorar. */
    private void giris() {
        final String kod = Db.duzenle(etKod.getText().toString());
        if (kod.isEmpty()) {
            etKod.setError("Araç kodunu yazın");
            etKod.requestFocus();
            return;
        }
        if (db.aracVar(kod)) {
            aracAc(kod);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(kod)
                    .setMessage("Bu araç daha önce kaydedilmemiş.\n\nYeni araç olarak kaydedilsin mi?")
                    .setPositiveButton("Kaydet ve Aç", (d, w) -> {
                        db.aracEkle(kod);
                        aracAc(kod);
                    })
                    .setNegativeButton("Vazgeç", null)
                    .show();
        }
    }

    private void aracAc(String kod) {
        Yardim.klavyeyiKapat(this);
        etKod.setText("");
        etKod.setError(null);
        Intent i = new Intent(this, VehicleActivity.class);
        i.putExtra(VehicleActivity.EK_KOD, kod);
        startActivity(i);
    }

    private void listeyiYenile() {
        String filtre = Db.duzenle(etKod.getText().toString());
        List<Db.Arac> liste = db.araclar(filtre);
        adapter.ayarla(liste);

        if (filtre.isEmpty()) {
            tvListeBaslik.setText("Kayıtlı araçlar (" + liste.size() + ")");
            tvBos.setText("Henüz kayıtlı araç yok.\n\nYukarıya araç kodunu yazıp GİRİŞ butonuna basın.");
        } else {
            tvListeBaslik.setText("Eşleşen araçlar (" + liste.size() + ")");
            tvBos.setText("\"" + filtre + "\" kayıtlı değil.\n\nGİRİŞ butonuna basarak yeni araç olarak kaydedebilirsiniz.");
        }
        boolean bos = liste.isEmpty();
        lvAraclar.setVisibility(bos ? View.GONE : View.VISIBLE);
        tvBos.setVisibility(bos ? View.VISIBLE : View.GONE);
    }

    // ---------------- Menü: yedekleme ----------------

    private void menuGoster(View anchor) {
        PopupMenu m = new PopupMenu(this, anchor);
        m.getMenu().add(0, 1, 0, "Yedek al (dosyaya kaydet)");
        m.getMenu().add(0, 2, 1, "Yedekten geri yükle");
        m.getMenu().add(0, 3, 2, "Hakkında");
        m.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: yedekAl(); return true;
                case 2: yedekSec(); return true;
                case 3: hakkinda(); return true;
            }
            return false;
        });
        m.show();
    }

    private void yedekAl() {
        String ad = "arac_denetim_yedek_"
                + new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(new Date()) + ".json";
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, ad);
        try {
            startActivityForResult(i, ISTEK_YEDEK_AL);
        } catch (Exception e) {
            Yardim.mesaj(this, "Dosya kaydetme ekranı açılamadı.");
        }
    }

    private void yedekSec() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        try {
            startActivityForResult(i, ISTEK_YEDEK_YUKLE);
        } catch (Exception e) {
            Yardim.mesaj(this, "Dosya seçme ekranı açılamadı.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == ISTEK_YEDEK_AL) {
            yedegiYaz(uri);
        } else if (requestCode == ISTEK_YEDEK_YUKLE) {
            yedegiOku(uri);
        }
    }

    private void yedegiYaz(Uri uri) {
        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
            if (os == null) throw new Exception("Dosya açılamadı");
            os.write(db.yedek().toString(2).getBytes(StandardCharsets.UTF_8));
            os.flush();
            Yardim.mesaj(this, "Yedek kaydedildi.");
        } catch (Exception e) {
            Yardim.mesaj(this, "Yedek alınamadı: " + e.getMessage());
        }
    }

    private void yedegiOku(Uri uri) {
        final JSONObject kok;
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) throw new Exception("Dosya açılamadı");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] tampon = new byte[8192];
            int n;
            while ((n = is.read(tampon)) != -1) bos.write(tampon, 0, n);
            kok = new JSONObject(new String(bos.toByteArray(), StandardCharsets.UTF_8));
            kok.getJSONArray("araclar");
            kok.getJSONArray("kayitlar");
        } catch (Exception e) {
            Yardim.mesaj(this, "Bu dosya geçerli bir yedek değil.");
            return;
        }

        int aracSayisi = kok.optJSONArray("araclar").length();
        int kayitSayisi = kok.optJSONArray("kayitlar").length();
        new AlertDialog.Builder(this)
                .setTitle("Yedekten geri yükle")
                .setMessage("Yedekte " + aracSayisi + " araç ve " + kayitSayisi + " kayıt var.\n\n"
                        + "DİKKAT: Telefondaki mevcut veriler silinip yerine yedek yüklenecek.")
                .setPositiveButton("Yükle", (d, w) -> {
                    try {
                        db.yedektenYukle(kok);
                        listeyiYenile();
                        Yardim.mesaj(this, "Yedek yüklendi.");
                    } catch (Exception e) {
                        Yardim.mesaj(this, "Yükleme başarısız: " + e.getMessage());
                    }
                })
                .setNegativeButton("Vazgeç", null)
                .show();
    }

    private void hakkinda() {
        new AlertDialog.Builder(this)
                .setTitle("Araç Denetim Defteri")
                .setMessage("Sürüm 1.0\n\n"
                        + "Tüm veriler yalnızca bu telefonda saklanır. İnternet kullanılmaz.\n\n"
                        + "Telefon değiştirmeden veya uygulamayı silmeden önce menüden "
                        + "\"Yedek al\" ile verilerinizi dosyaya kaydedin. Yeni telefonda "
                        + "\"Yedekten geri yükle\" ile geri alabilirsiniz.")
                .setPositiveButton("Tamam", null)
                .show();
    }

    // ---------------- Liste ----------------

    private class AracAdapter extends BaseAdapter {
        private List<Db.Arac> liste = new ArrayList<>();

        void ayarla(List<Db.Arac> yeni) {
            liste = yeni;
            notifyDataSetChanged();
        }

        @Override public int getCount() { return liste.size(); }
        @Override public Db.Arac getItem(int i) { return liste.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View v, ViewGroup parent) {
            if (v == null) {
                v = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_arac, parent, false);
            }
            Db.Arac a = getItem(pos);
            ((TextView) v.findViewById(R.id.tvKod)).setText(a.kod);

            String bilgi = a.toplam == 0
                    ? "Henüz kayıt yok"
                    : a.toplam + " kayıt  •  Son: " + Yardim.tarih(a.sonTarih);
            ((TextView) v.findViewById(R.id.tvBilgi)).setText(bilgi);

            TextView tvAcik = v.findViewById(R.id.tvAcik);
            if (a.acik > 0) {
                tvAcik.setVisibility(View.VISIBLE);
                tvAcik.setText(a.acik + " açık");
            } else {
                tvAcik.setVisibility(View.GONE);
            }
            return v;
        }
    }
}
