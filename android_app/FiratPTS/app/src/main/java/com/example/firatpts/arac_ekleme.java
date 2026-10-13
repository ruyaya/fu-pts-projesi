package com.example.firatpts;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.switchmaterial.SwitchMaterial;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class arac_ekleme extends AppCompatActivity {
    private EditText etPlaka, etSahip;
    private Spinner spnDurum;
    private ImageView ivAracGorseli;
    private SwitchMaterial swYasakli;
    private String url = "http://127.0.0.1:5000/api/arac-ekle";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arac_ekleme);
        etPlaka = findViewById(R.id.etPlaka);
        etSahip = findViewById(R.id.etSahip);
        spnDurum = findViewById(R.id.spnDurum);
        ivAracGorseli = findViewById(R.id.ivAracGorseli);
        swYasakli = findViewById(R.id.swYasakli);
        Button btnKaydet = findViewById(R.id.btnAracKaydet);

        String gelenPlaka = getIntent().getStringExtra("PLAKA_NO");
        if (gelenPlaka != null) {
            etPlaka.setText(gelenPlaka);
        }

        if (DashBoardActivity.sonAracGorseli != null) {
            ivAracGorseli.setImageBitmap(DashBoardActivity.sonAracGorseli);
            ivAracGorseli.setColorFilter(null);
        } else {
            Toast.makeText(this, "Görsel yüklenemedi!", Toast.LENGTH_SHORT).show();
        }
        String[] durumlar = {"Akademik Personel", "İdari Personel", "Öğrenci", "Misafir"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, durumlar) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getView(position, convertView, parent);
                ((android.widget.TextView) view).setTextColor(android.graphics.Color.WHITE);
                return view;
            }
            @Override
            public android.view.View getDropDownView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getDropDownView(position, convertView, parent);
                view.setBackgroundColor(android.graphics.Color.parseColor("#121212"));
                ((android.widget.TextView) view).setTextColor(android.graphics.Color.WHITE);
                return view;
            }
        };
        spnDurum.setAdapter(adapter);
        btnKaydet.setOnClickListener(v -> kaydet());
    }
    private void kaydet() {
        String plaka = etPlaka.getText().toString().trim().toUpperCase();
        String sahip = etSahip.getText().toString().trim();
        String durum = spnDurum.getSelectedItem().toString();
        String gecisIzni = swYasakli.isChecked() ? "YASAKLI" : "ONAYLI";
        if (plaka.isEmpty() || sahip.isEmpty()) {
            Toast.makeText(this, "Tüm alanları doldurun!", Toast.LENGTH_SHORT).show();
            return;
        }
        JSONObject veri = new JSONObject();
        try {
            veri.put("plaka", plaka);
            veri.put("sahip", sahip);
            veri.put("durum", durum);
            veri.put("gecis_izni", gecisIzni);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, veri,
                response -> {
                    Toast.makeText(this, "Araç Başarıyla Kaydedildi", Toast.LENGTH_SHORT).show();
                    DashBoardActivity.sonAracGorseli = null;
                    finish();
                },
                error -> Toast.makeText(this, "Kayıt Hatası! Sunucu kapalı olabilir.", Toast.LENGTH_SHORT).show()) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("ngrok-skip-browser-warning", "true");
                return h;
            }
        };
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                10000,
                1,
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(this).add(request);
    }
}