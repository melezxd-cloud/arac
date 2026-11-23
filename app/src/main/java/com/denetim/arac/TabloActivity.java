package com.denetim.arac;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;

/** Amirlik denetim sayıları tablosunu gösterir. */
public class TabloActivity extends Activity {

    private ImageView img;
    private boolean yakin = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tablo);

        img = findViewById(R.id.imgTablo);
        findViewById(R.id.btnGeri).setOnClickListener(v -> finish());
        findViewById(R.id.btnZoom).setOnClickListener(v -> zoomCevir());

        // Açılışta yazılar okunabilsin diye yakın başlar
        img.post(this::olcuyuUygula);
    }

    private void zoomCevir() {
        yakin = !yakin;
        olcuyuUygula();
    }

    private void olcuyuUygula() {
        int ekran = getResources().getDisplayMetrics().widthPixels;
        ViewGroup.LayoutParams lp = img.getLayoutParams();
        // Yakın: ekranın iki buçuk katı genişlik, kaydırarak gezilir.
        // Uzak: tamamı ekrana sığar.
        lp.width = yakin ? (int) (ekran * 2.5f) : ekran;
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        img.setLayoutParams(lp);
        img.setAdjustViewBounds(true);
    }
}
