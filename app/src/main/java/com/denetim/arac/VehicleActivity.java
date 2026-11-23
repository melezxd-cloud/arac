package com.denetim.arac;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Araç ekranı: aracın geçmiş denetim kayıtları ve yeni kayıt ekleme. */
public class VehicleActivity extends Activity {

    public static final String EK_KOD = "kod";

    private Db db;
    private String kod;
    private TextView tvOzet;
    private TextView tvBos;
    private ListView lvKayitlar;
    private RadioGroup rgFiltre;
    private final KayitAdapter adapter = new KayitAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        kod = getIntent().getStringExtra(EK_KOD);
        if (kod == null || kod.isEmpty()) {
            finish();
            return;
        }
        setContentView(R.layout.activity_vehicle);
        db = Db.al(this);

        ((TextView) findViewById(R.id.tvKod)).setText(kod);
        tvOzet = findViewById(R.id.tvOzet);
        tvBos = findViewById(R.id.tvBos);
        lvKayitlar = findViewById(R.id.lvKayitlar);
        rgFiltre = findViewById(R.id.rgFiltre);

        lvKayitlar.setAdapter(adapter);
        lvKayitlar.setOnItemClickListener((p, v, pos, id) -> {
            Intent i = new Intent(this, RecordActivity.class);
            i.putExtra(RecordActivity.EK_ID, adapter.getItem(pos).id);
            startActivity(i);
        });

        findViewById(R.id.btnGeri).setOnClickListener(v -> finish());
        findViewById(R.id.btnMenu).setOnClickListener(this::menuGoster);
        findViewById(R.id.btnYeni).setOnClickListener(v -> {
            Intent i = new Intent(this, RecordActivity.class);
            i.putExtra(RecordActivity.EK_KOD, kod);
            startActivity(i);
        });
        rgFiltre.setOnCheckedChangeListener((g, id) -> yenile());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (db != null) yenile();
    }

    private int filtre() {
        int id = rgFiltre.getCheckedRadioButtonId();
        if (id == R.id.rbAcik) return Db.FILTRE_ACIK;
        if (id == R.id.rbGiderilen) return Db.FILTRE_GIDERILEN;
        return Db.FILTRE_TUMU;
    }

    private void yenile() {
        Db.Arac a = db.arac(kod);
        if (a == null) { // araç silinmiş
            finish();
            return;
        }

        if (a.toplam == 0) {
            tvOzet.setText("Bu araç için henüz denetim kaydı yok.\n"
                    + "İlk kaydı eklemek için aşağıdaki butona basın.");
        } else {
            String ozet = "Toplam kayıt: " + a.toplam
                    + "\nAçık (giderilmemiş) sorun: " + a.acik + (a.acik > 0 ? "  ⚠" : "")
                    + "\nSon denetim: " + Yardim.tarihSaat(a.sonTarih);
            tvOzet.setText(ozet);
        }

        int f = filtre();
        List<Db.Kayit> liste = db.kayitlar(kod, f);
        adapter.ayarla(liste);

        boolean bos = liste.isEmpty();
        if (f == Db.FILTRE_ACIK) tvBos.setText("Açık sorun yok. ✓");
        else if (f == Db.FILTRE_GIDERILEN) tvBos.setText("Giderilmiş kayıt yok.");
        else tvBos.setText("Bu araç için kayıt yok.");
        lvKayitlar.setVisibility(bos ? View.GONE : View.VISIBLE);
        tvBos.setVisibility(bos ? View.VISIBLE : View.GONE);
    }

    // ---------------- Menü ----------------

    private void menuGoster(View anchor) {
        PopupMenu m = new PopupMenu(this, anchor);
        m.getMenu().add(0, 1, 0, "Geçmişi paylaş / gönder");
        m.getMenu().add(0, 2, 1, "Aracı sil");
        m.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) { paylas(); return true; }
            if (item.getItemId() == 2) { aracSilSor(); return true; }
            return false;
        });
        m.show();
    }

    /** Aracın tüm geçmişini düz metin olarak paylaşır (WhatsApp, SMS, e-posta, not uygulaması...). */
    private void paylas() {
        Db.Arac a = db.arac(kod);
        List<Db.Kayit> liste = db.kayitlar(kod, Db.FILTRE_TUMU);
        StringBuilder sb = new StringBuilder();
        sb.append("ARAÇ: ").append(kod).append('\n');
        sb.append("Rapor tarihi: ").append(Yardim.tarihSaat(System.currentTimeMillis())).append('\n');
        if (a != null) {
            sb.append("Toplam ").append(a.toplam).append(" kayıt, ")
              .append(a.acik).append(" açık sorun\n");
        }
        sb.append("------------------------------\n");
        for (Db.Kayit k : liste) {
            sb.append(Yardim.tarihSaat(k.tarih)).append("  |  ").append(Yardim.turVeBolum(k)).append('\n');
            if (k.notlar != null && !k.notlar.isEmpty()) sb.append(k.notlar).append('\n');
            if (Yardim.KONTROL.equals(k.tur)) {
                sb.append("Durum: SORUN YOK\n");
            } else if (k.giderildi) {
                sb.append("Durum: GİDERİLDİ");
                if (k.giderilme > 0) sb.append(" (").append(Yardim.tarih(k.giderilme)).append(')');
                sb.append('\n');
            } else {
                sb.append("Durum: AÇIK\n");
            }
            sb.append('\n');
        }

        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, kod + " denetim geçmişi");
        i.putExtra(Intent.EXTRA_TEXT, sb.toString());
        try {
            startActivity(Intent.createChooser(i, "Paylaş"));
        } catch (Exception e) {
            Yardim.mesaj(this, "Paylaşılacak uygulama bulunamadı.");
        }
    }

    private void aracSilSor() {
        Db.Arac a = db.arac(kod);
        int n = a == null ? 0 : a.toplam;
        new AlertDialog.Builder(this)
                .setTitle(kod + " silinsin mi?")
                .setMessage("Araç ve ona ait " + n + " kayıt kalıcı olarak silinecek. Bu işlem geri alınamaz.")
                .setPositiveButton("Sil", (d, w) -> {
                    db.aracSil(kod);
                    Yardim.mesaj(this, kod + " silindi.");
                    finish();
                })
                .setNegativeButton("Vazgeç", null)
                .show();
    }

    // ---------------- Liste ----------------

    private class KayitAdapter extends BaseAdapter {
        private List<Db.Kayit> liste = new ArrayList<>();

        void ayarla(List<Db.Kayit> yeni) {
            liste = yeni;
            notifyDataSetChanged();
        }

        @Override public int getCount() { return liste.size(); }
        @Override public Db.Kayit getItem(int i) { return liste.get(i); }
        @Override public long getItemId(int i) { return liste.get(i).id; }

        @Override
        public View getView(int pos, View v, ViewGroup parent) {
            if (v == null) {
                v = LayoutInflater.from(VehicleActivity.this).inflate(R.layout.item_kayit, parent, false);
            }
            Db.Kayit k = getItem(pos);

            ((TextView) v.findViewById(R.id.tvTur)).setText(Yardim.turVeBolum(k));
            ((TextView) v.findViewById(R.id.tvTarih)).setText(Yardim.tarihSaat(k.tarih));

            TextView tvNot = v.findViewById(R.id.tvNot);
            if (k.notlar == null || k.notlar.isEmpty()) {
                tvNot.setVisibility(View.GONE);
            } else {
                tvNot.setVisibility(View.VISIBLE);
                tvNot.setText(k.notlar);
            }

            TextView tvDurum = v.findViewById(R.id.tvDurum);
            View serit = v.findViewById(R.id.serit);
            if (k.giderildi) {
                String s;
                if (Yardim.KONTROL.equals(k.tur)) {
                    s = "✓ SORUN YOK";
                } else {
                    s = "✓ GİDERİLDİ";
                    if (k.giderilme > 0) s += " " + Yardim.tarih(k.giderilme);
                }
                tvDurum.setText(s);
                tvDurum.setTextColor(getColor(R.color.gri));
                serit.setBackgroundColor(getColor(R.color.gri_cizgi));
            } else {
                tvDurum.setText("AÇIK");
                tvDurum.setTextColor(getColor(R.color.kirmizi));
                serit.setBackgroundColor(getColor(R.color.kirmizi));
            }
            return v;
        }
    }
}
