package com.vakit.namaz;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 81 il listesi; aramayla süzülür, seçilen il kaydedilir. */
public class IlActivity extends Activity {

    private static final Locale TR_DIL = Locale.forLanguageTag("tr-TR");

    private final List<Integer> gosterilen = new ArrayList<>();
    private final IlAdapter adapter = new IlAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_il);

        ListView lv = findViewById(R.id.lvIller);
        EditText etArama = findViewById(R.id.etArama);

        suz("");
        lv.setAdapter(adapter);
        lv.setOnItemClickListener((p, v, konum, id) -> {
            int il = gosterilen.get(konum);
            Depo.ilAyarla(this, il);
            AlarmPlanlayici.yenidenKur(this);
            setResult(RESULT_OK);
            finish();
        });

        etArama.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { suz(s.toString()); }
        });
    }

    private void suz(String arama) {
        String a = sadelestir(arama);
        gosterilen.clear();
        for (int i = 0; i < Sehirler.sayi(); i++) {
            if (a.isEmpty() || sadelestir(Sehirler.ad(i)).contains(a)) {
                gosterilen.add(i);
            }
        }
        adapter.notifyDataSetChanged();
    }

    /** Türkçe harfleri sadeleştirir: "Şanlıurfa" yazarken "sanliurfa" da bulsun. */
    private static String sadelestir(String s) {
        if (s == null) return "";
        String t = s.toLowerCase(TR_DIL)
                .replace('ı', 'i').replace('ğ', 'g').replace('ü', 'u')
                .replace('ş', 's').replace('ö', 'o').replace('ç', 'c');
        t = Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return t.trim();
    }

    private class IlAdapter extends BaseAdapter {
        @Override public int getCount() { return gosterilen.size(); }
        @Override public Integer getItem(int i) { return gosterilen.get(i); }
        @Override public long getItemId(int i) { return gosterilen.get(i); }

        @Override
        public View getView(int konum, View v, ViewGroup parent) {
            if (v == null) {
                v = LayoutInflater.from(IlActivity.this)
                        .inflate(R.layout.item_il, parent, false);
            }
            int il = gosterilen.get(konum);
            TextView tv = (TextView) v;
            boolean secili = (il == Depo.il(IlActivity.this));
            tv.setText(secili ? Sehirler.ad(il) + "  ✓" : Sehirler.ad(il));
            tv.setTextColor(getColor(secili ? R.color.altin : R.color.krem_yumusak));
            return v;
        }
    }
}
