package com.denetim.arac;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Calendar;

/** Yeni kayıt ekleme veya mevcut kaydı düzenleme ekranı. */
public class RecordActivity extends Activity {

    public static final String EK_KOD = "kod";
    public static final String EK_ID = "id";

    private Db db;
    private Db.Kayit kayit;
    private boolean yeni;
    private final Calendar tarih = Calendar.getInstance();

    private TextView tvTarih;
    private Spinner spTur;
    private Spinner spBolum;
    private EditText etNot;
    private RadioButton rbAcik;
    private RadioButton rbGiderildi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = Db.al(this);

        long id = getIntent().getLongExtra(EK_ID, 0);
        if (id != 0) {
            kayit = db.kayit(id);
            if (kayit == null) {
                finish();
                return;
            }
            yeni = false;
        } else {
            String kod = getIntent().getStringExtra(EK_KOD);
            if (kod == null || kod.isEmpty()) {
                finish();
                return;
            }
            kayit = new Db.Kayit();
            kayit.kod = kod;
            kayit.tarih = System.currentTimeMillis();
            kayit.tur = Yardim.TURLER[0];
            kayit.bolum = Yardim.BOLUMLER[0];
            yeni = true;
        }

        setContentView(R.layout.activity_record);

        ((TextView) findViewById(R.id.tvBaslik)).setText(yeni ? "Yeni Kayıt" : "Kaydı Düzenle");
        ((TextView) findViewById(R.id.tvArac)).setText(kayit.kod);
        tvTarih = findViewById(R.id.tvTarih);
        spTur = findViewById(R.id.spTur);
        spBolum = findViewById(R.id.spBolum);
        etNot = findViewById(R.id.etNot);
        rbAcik = findViewById(R.id.rbAcik);
        rbGiderildi = findViewById(R.id.rbGiderildi);

        spTur.setAdapter(listeAdapter(Yardim.TURLER));
        spBolum.setAdapter(listeAdapter(Yardim.BOLUMLER));
        spTur.setPrompt("Kayıt türü");
        spBolum.setPrompt("Aracın bölümü");

        // Mevcut değerleri ekrana yerleştir
        tarih.setTimeInMillis(kayit.tarih);
        tarihiGoster();
        spTur.setSelection(sira(Yardim.TURLER, kayit.tur, Yardim.TURLER.length - 1));
        spBolum.setSelection(sira(Yardim.BOLUMLER, kayit.bolum, 0));
        etNot.setText(kayit.notlar == null ? "" : kayit.notlar);
        if (kayit.giderildi) rbGiderildi.setChecked(true);
        else rbAcik.setChecked(true);

        // "Kontrol – sorun yok" seçilince durum otomatik "Giderildi" olur
        spTur.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (Yardim.KONTROL.equals(Yardim.TURLER[pos])) rbGiderildi.setChecked(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> p) { }
        });

        findViewById(R.id.btnGeri).setOnClickListener(v -> geriDon());
        findViewById(R.id.btnTarih).setOnClickListener(v -> tarihSec());
        findViewById(R.id.btnKaydet).setOnClickListener(v -> kaydet());

        Button btnSil = findViewById(R.id.btnSil);
        btnSil.setVisibility(yeni ? View.GONE : View.VISIBLE);
        btnSil.setOnClickListener(v -> silSor());

        if (yeni) etNot.requestFocus();
    }

    private ArrayAdapter<String> listeAdapter(String[] ogeler) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this, R.layout.spinner_oge, ogeler);
        a.setDropDownViewResource(R.layout.spinner_oge);
        return a;
    }

    private static int sira(String[] dizi, String deger, int varsayilan) {
        int i = Arrays.asList(dizi).indexOf(deger);
        return i >= 0 ? i : varsayilan;
    }

    private void tarihiGoster() {
        tvTarih.setText(Yardim.tarihSaat(tarih.getTimeInMillis()));
    }

    /** Önce tarih, sonra saat seçtirir (geçmiş bir denetimi sonradan girmek için). */
    private void tarihSec() {
        new DatePickerDialog(this, (dp, yil, ay, gun) -> {
            tarih.set(Calendar.YEAR, yil);
            tarih.set(Calendar.MONTH, ay);
            tarih.set(Calendar.DAY_OF_MONTH, gun);
            tarihiGoster();
            new TimePickerDialog(this, (tp, saat, dakika) -> {
                tarih.set(Calendar.HOUR_OF_DAY, saat);
                tarih.set(Calendar.MINUTE, dakika);
                tarihiGoster();
            }, tarih.get(Calendar.HOUR_OF_DAY), tarih.get(Calendar.MINUTE), true).show();
        }, tarih.get(Calendar.YEAR), tarih.get(Calendar.MONTH), tarih.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void kaydet() {
        String tur = Yardim.TURLER[spTur.getSelectedItemPosition()];
        String bolum = Yardim.BOLUMLER[spBolum.getSelectedItemPosition()];
        String not = etNot.getText().toString().trim();

        if (not.isEmpty() && !Yardim.KONTROL.equals(tur)) {
            etNot.setError("Kısa bir açıklama yazın");
            etNot.requestFocus();
            return;
        }

        boolean giderildi = rbGiderildi.isChecked();
        if (giderildi && !kayit.giderildi) kayit.giderilme = System.currentTimeMillis();
        if (!giderildi) kayit.giderilme = 0;

        kayit.giderildi = giderildi;
        kayit.tur = tur;
        kayit.bolum = bolum;
        kayit.notlar = not;
        kayit.tarih = tarih.getTimeInMillis();
        db.kaydet(kayit);

        Yardim.klavyeyiKapat(this);
        Yardim.mesaj(this, "Kaydedildi.");
        finish();
    }

    private void silSor() {
        new AlertDialog.Builder(this)
                .setTitle("Kayıt silinsin mi?")
                .setMessage("Bu kayıt kalıcı olarak silinecek.")
                .setPositiveButton("Sil", (d, w) -> {
                    db.kayitSil(kayit.id);
                    Yardim.mesaj(this, "Kayıt silindi.");
                    finish();
                })
                .setNegativeButton("Vazgeç", null)
                .show();
    }

    /** Yazılmış ama kaydedilmemiş not varsa çıkmadan önce sorar. */
    private void geriDon() {
        String not = etNot.getText().toString().trim();
        String eski = kayit.notlar == null ? "" : kayit.notlar;
        if (!not.equals(eski)) {
            new AlertDialog.Builder(this)
                    .setMessage("Değişiklikler kaydedilmedi. Çıkılsın mı?")
                    .setPositiveButton("Kaydetmeden çık", (d, w) -> finish())
                    .setNegativeButton("Geri dön", null)
                    .show();
        } else {
            finish();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        geriDon();
    }
}
