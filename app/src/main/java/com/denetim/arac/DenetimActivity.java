package com.denetim.arac;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** O gün o peronda yapılması gereken en az denetim sayısını gösterir. */
public class DenetimActivity extends Activity {

    private static final Locale TR_DIL = Locale.forLanguageTag("tr-TR");

    private Db db;
    private Spinner spPeron;
    private Button btnTekli, btnCiftli;
    private TextView tvGun, tvSayi, tvSayiAlt, tvIlerleme, tvKalan, tvListeBaslik;
    private LinearLayout peronListesi;

    private boolean ciftli;
    private int gun;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_denetim);
        db = Db.al(this);

        spPeron = findViewById(R.id.spPeron);
        btnTekli = findViewById(R.id.btnTekli);
        btnCiftli = findViewById(R.id.btnCiftli);
        tvGun = findViewById(R.id.tvGun);
        tvSayi = findViewById(R.id.tvSayi);
        tvSayiAlt = findViewById(R.id.tvSayiAlt);
        tvIlerleme = findViewById(R.id.tvIlerleme);
        tvKalan = findViewById(R.id.tvKalan);
        tvListeBaslik = findViewById(R.id.tvListeBaslik);
        peronListesi = findViewById(R.id.peronListesi);

        ciftli = Yardim.ciftli(this);
        gun = Peron.bugununGunu();

        ArrayAdapter<String> a = new ArrayAdapter<>(this, R.layout.spinner_oge, Peron.AD);
        a.setDropDownViewResource(R.layout.spinner_oge);
        spPeron.setAdapter(a);
        spPeron.setPrompt("Peron seçin");
        spPeron.setSelection(Yardim.peron(this));
        spPeron.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                Yardim.peronAyarla(DenetimActivity.this, pos);
                yenile();
            }

            @Override
            public void onNothingSelected(AdapterView<?> p) { }
        });

        btnTekli.setOnClickListener(v -> vardiya(false));
        btnCiftli.setOnClickListener(v -> vardiya(true));

        findViewById(R.id.btnGeri).setOnClickListener(v -> finish());
        findViewById(R.id.btnTablo).setOnClickListener(v -> tabloyuAc());
        findViewById(R.id.btnTabloAc).setOnClickListener(v -> tabloyuAc());

        SimpleDateFormat b = new SimpleDateFormat("EEEE, d MMMM yyyy", TR_DIL);
        tvGun.setText(b.format(new Date()) + "  ·  " + Peron.GUN_ADI[gun]);
    }

    @Override
    protected void onResume() {
        super.onResume();
        yenile();
    }

    private void tabloyuAc() {
        startActivity(new Intent(this, TabloActivity.class));
    }

    private void vardiya(boolean yeni) {
        ciftli = yeni;
        Yardim.ciftliAyarla(this, yeni);
        yenile();
    }

    private void yenile() {
        // Tekli / çiftli düğmelerinin görünümü
        btnTekli.setBackgroundResource(ciftli ? R.drawable.buton_cerceve : R.drawable.buton_kirmizi);
        btnTekli.setTextColor(getColor(ciftli ? R.color.kirmizi : R.color.beyaz));
        btnCiftli.setBackgroundResource(ciftli ? R.drawable.buton_kirmizi : R.drawable.buton_cerceve);
        btnCiftli.setTextColor(getColor(ciftli ? R.color.beyaz : R.color.kirmizi));

        int secili = spPeron.getSelectedItemPosition();
        if (secili < 0) secili = 0;

        int hedef = Peron.enAzDenetim(secili, gun, ciftli);
        tvSayi.setText(String.valueOf(hedef));
        tvSayiAlt.setText(Peron.ad(secili) + "  ·  " + Peron.GUN_ADI[gun].toLowerCase(TR_DIL)
                + "  ·  " + (ciftli ? "çiftli" : "tekli"));

        int yapilan = db.bugunDenetlenenArac();
        tvIlerleme.setText("Bugün kayıt girdiğin araç: " + yapilan + " / " + hedef);
        if (yapilan >= hedef) {
            tvKalan.setText("Hedefe ulaşıldı ✓");
            tvKalan.setTextColor(getColor(R.color.yesil));
        } else {
            tvKalan.setText("Kalan: " + (hedef - yapilan));
            tvKalan.setTextColor(getColor(R.color.gri));
        }

        listeyiKur(secili);
    }

    /** Bugün bütün peronların sayısını alt alta gösterir. */
    private void listeyiKur(int secili) {
        tvListeBaslik.setText("Bugün diğer peronlar · " + (ciftli ? "çiftli" : "tekli"));
        peronListesi.removeAllViews();

        LayoutInflater li = LayoutInflater.from(this);
        for (int i = 0; i < Peron.sayi(); i++) {
            View s = li.inflate(R.layout.item_denetim, peronListesi, false);
            TextView ad = s.findViewById(R.id.tvAd);
            TextView sayi = s.findViewById(R.id.tvSayi);

            ad.setText(Peron.ad(i));
            sayi.setText(String.valueOf(Peron.enAzDenetim(i, gun, ciftli)));

            if (i == secili) {
                s.setBackgroundResource(R.drawable.kutu_sonuc);
                ad.setTypeface(null, android.graphics.Typeface.BOLD);
                sayi.setTextColor(getColor(R.color.kirmizi));
            }
            peronListesi.addView(s);
        }
    }
}
