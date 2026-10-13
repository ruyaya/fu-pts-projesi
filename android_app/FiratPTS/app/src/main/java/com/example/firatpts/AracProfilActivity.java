package com.example.firatpts;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class AracProfilActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arac_profil);

        ImageView imgResim = findViewById(R.id.profilAracResmi);
        TextView txtPlaka = findViewById(R.id.profilPlaka);
        TextView txtSaat = findViewById(R.id.profilSaat);
        TextView txtSonuc = findViewById(R.id.profilSonuc);

        String gelenPlaka = getIntent().getStringExtra("PLAKA");
        String gelenSaat = getIntent().getStringExtra("SAAT");
        String gelenSonuc = getIntent().getStringExtra("SONUC");
        String gelenGorsel = getIntent().getStringExtra("GORSEL");

        if (gelenPlaka != null) txtPlaka.setText(gelenPlaka);
        if (gelenSaat != null) txtSaat.setText("Geçiş: " + gelenSaat);

        if (gelenSonuc != null) {
            txtSonuc.setText(gelenSonuc);
            if (gelenSonuc.contains("YASAKLI") || gelenSonuc.contains("YABANCI")) {
                txtSonuc.setTextColor(android.graphics.Color.RED);
            } else {
                txtSonuc.setTextColor(android.graphics.Color.GREEN);
            }
        }

        if (gelenGorsel != null && !gelenGorsel.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(gelenGorsel, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                imgResim.setImageBitmap(decodedByte);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}