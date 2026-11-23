package com.denetim.arac;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/**
 * Hızlı Not: peron seçilir, sorun yazılır, kopyalanmaya hazır metin üretilir.
 * Örnek çıktı: "Başakşehir Sys, Fenertepe Peronu Saat 15:00 ön cam çatlak olduğu tespit edildi."
 */
public class HizliNotActivity extends Activity {

    private static final Locale TR_DIL = Locale.forLanguageTag("tr-TR");

    private Db db;
    private Spinner spPeron;
    private EditText etSorun;
    private CheckBox cbOtoCumle;
    private LinearLayout kutuSonuc, gecmisListesi;
    private TextView tvSonuc, tvGecmisBos, tvGecmisBaslik;

    private String sonMetin = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hizli_not);
        db = Db.al(this);

        spPeron = findViewById(R.id.spPeron);
        etSorun = findViewById(R.id.etSorun);
        cbOtoCumle = findViewById(R.id.cbOtoCumle);
        kutuSonuc = findViewById(R.id.kutuSonuc);
        gecmisListesi = findViewById(R.id.gecmisListesi);
        tvSonuc = findViewById(R.id.tvSonuc);
        tvGecmisBos = findViewById(R.id.tvGecmisBos);
        tvGecmisBaslik = findViewById(R.id.tvGecmisBaslik);

        ArrayAdapter<String> a = new ArrayAdapter<>(this, R.layout.spinner_oge, Peron.AD);
        a.setDropDownViewResource(R.layout.spinner_oge);
        spPeron.setAdapter(a);
        spPeron.setPrompt("Peron seçin");
        spPeron.setSelection(Yardim.peron(this));

        // Seçilen peron hatırlansın; bir dahaki açılışta hazır gelsin
        spPeron.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                Yardim.peronAyarla(HizliNotActivity.this, pos);
            }

            @Override
            public void onNothingSelected(AdapterView<?> p) { }
        });

        cbOtoCumle.setChecked(Yardim.otomatikCumle(this));
        cbOtoCumle.setOnCheckedChangeListener(
                (b, acik) -> Yardim.otomatikCumleAyarla(this, acik));

        findViewById(R.id.btnGeri).setOnClickListener(v -> finish());
        findViewById(R.id.btnTesis).setOnClickListener(v -> tesisiDegistir());
        findViewById(R.id.btnTamam).setOnClickListener(v -> metniUret());
        findViewById(R.id.btnKopyala).setOnClickListener(v -> Yardim.panoyaKopyala(this, sonMetin));
        findViewById(R.id.btnPaylas).setOnClickListener(v -> paylas(sonMetin));
        findViewById(R.id.btnYeni).setOnClickListener(v -> temizle());

        gecmisiYenile();
    }

    /** Hazır metni kurar, kaydeder ve ekranda gösterir. */
    private void metniUret() {
        String sorun = etSorun.getText().toString().trim();
        if (sorun.isEmpty()) {
            etSorun.setError("Sorunu yazın");
            etSorun.requestFocus();
            return;
        }

        int peronNo = spPeron.getSelectedItemPosition();
        String peron = Peron.ad(peronNo);
        String metin = Yardim.tesis(this) + ", " + peron
                + " Saat " + Yardim.saat(System.currentTimeMillis()) + " "
                + cumleyiTamamla(sorun);

        sonMetin = metin;
        tvSonuc.setText(metin);
        kutuSonuc.setVisibility(View.VISIBLE);

        db.hizliNotEkle(peron, metin);
        Yardim.panoyaKopyala(this, metin);
        Yardim.klavyeyiKapat(this);
        gecmisiYenile();
    }

    /**
     * Onay kutusu açıksa cümlenin sonuna "tespit edildi." ekler.
     * Kullanıcı zaten tam cümle yazdıysa ikinci kez eklemez.
     */
    private String cumleyiTamamla(String sorun) {
        if (!cbOtoCumle.isChecked()) return sorun;

        String kucuk = sorun.toLowerCase(TR_DIL);
        if (kucuk.contains("tespit edil")) return sorun;

        String s = sorun;
        while (s.endsWith(".") || s.endsWith(",")) {
            s = s.substring(0, s.length() - 1).trim();
        }
        // "ön cam çatlak olduğu" gibi yazıldıysa sondaki "olduğu" atılır
        if (s.toLowerCase(TR_DIL).endsWith("olduğu")) {
            s = s.substring(0, s.length() - 6).trim();
        }
        return s + " tespit edildi.";
    }

    private void temizle() {
        etSorun.setText("");
        etSorun.setError(null);
        kutuSonuc.setVisibility(View.GONE);
        sonMetin = "";
        etSorun.requestFocus();
    }

    private void paylas(String metin) {
        if (metin == null || metin.isEmpty()) return;
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, metin);
        try {
            startActivity(Intent.createChooser(i, "Paylaş"));
        } catch (Exception e) {
            Yardim.mesaj(this, "Paylaşılacak uygulama bulunamadı.");
        }
    }

    /** Hazır metnin başındaki tesis adını değiştirmeye yarar. */
    private void tesisiDegistir() {
        final EditText kutu = new EditText(this);
        kutu.setText(Yardim.tesis(this));
        kutu.setSelectAllOnFocus(true);
        kutu.setTextColor(getColor(R.color.siyah));

        int p = (int) (18 * getResources().getDisplayMetrics().density);
        kutu.setPadding(p, p / 2, p, p / 2);

        new AlertDialog.Builder(this)
                .setTitle("Tesis adı")
                .setMessage("Hazır metnin başına bu ad gelir.")
                .setView(kutu)
                .setPositiveButton("Kaydet", (d, w) -> {
                    String yeni = kutu.getText().toString().trim();
                    if (!yeni.isEmpty()) {
                        Yardim.tesisAyarla(this, yeni);
                        Yardim.mesaj(this, "Tesis adı güncellendi.");
                    }
                })
                .setNegativeButton("Vazgeç", null)
                .show();
    }

    // ---------------- Bugünün notları ----------------

    private void gecmisiYenile() {
        gecmisListesi.removeAllViews();
        List<Db.HizliNot> liste = db.bugunkuHizliNotlar();

        tvGecmisBaslik.setText("Bugün yazdıkların (" + liste.size() + ")");
        tvGecmisBos.setVisibility(liste.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater li = LayoutInflater.from(this);
        for (final Db.HizliNot h : liste) {
            View s = li.inflate(R.layout.item_hizlinot, gecmisListesi, false);
            ((TextView) s.findViewById(R.id.tvUst))
                    .setText(Yardim.saat(h.tarih) + " · " + h.peron);
            ((TextView) s.findViewById(R.id.tvMetin)).setText(h.metin);

            s.findViewById(R.id.btnKopyalaSatir)
                    .setOnClickListener(v -> Yardim.panoyaKopyala(this, h.metin));

            s.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setMessage("Bu not silinsin mi?")
                        .setPositiveButton("Sil", (d, w) -> {
                            db.hizliNotSil(h.id);
                            gecmisiYenile();
                        })
                        .setNegativeButton("Vazgeç", null)
                        .show();
                return true;
            });

            gecmisListesi.addView(s);
        }
    }
}
