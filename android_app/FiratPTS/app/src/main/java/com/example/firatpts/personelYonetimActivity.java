package com.example.firatpts;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class personelYonetimActivity extends AppCompatActivity {

    private EditText etKullanici, etSifre;
    private RecyclerView rvListe;
    private KullaniciAdapter adapter;
    private List<Kullanici> kullanicilar = new ArrayList<>();
    private String baseUrl = "http://127.0.0.1:5000/api";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personel_yonetim);

        etKullanici = findViewById(R.id.etYeniKullanici);
        etSifre = findViewById(R.id.etYeniSifre);
        Button btnEkle = findViewById(R.id.btnGorevliEkle);
        rvListe = findViewById(R.id.rvGorevliListesi);

        rvListe.setLayoutManager(new LinearLayoutManager(this));
        personelleriGetir();

        btnEkle.setOnClickListener(v -> personelEkle());
    }

    private void personelleriGetir() {
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, baseUrl + "/kullanicilar", null,
                response -> {
                    kullanicilar.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            kullanicilar.add(new Kullanici(obj.getInt("id"), obj.getString("kullanici_adi"), obj.getString("rol")));
                        } catch (JSONException e) { e.printStackTrace(); }
                    }
                    adapter = new KullaniciAdapter(kullanicilar, this::personelSil);
                    rvListe.setAdapter(adapter);
                },
                error -> Toast.makeText(this, "Liste yüklenemedi", Toast.LENGTH_SHORT).show()) {
            @Override public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>(); h.put("ngrok-skip-browser-warning", "true"); return h;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }

    private void personelEkle() {
        String kadi = etKullanici.getText().toString().trim();
        String sifre = etSifre.getText().toString().trim();
        if(kadi.isEmpty() || sifre.isEmpty()) return;

        JSONObject params = new JSONObject();
        try { params.put("kullanici_adi", kadi); params.put("sifre", sifre); } catch (Exception e) {}

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, baseUrl + "/kullanici-ekle", params,
                response -> {
                    Toast.makeText(this, "Personel eklendi", Toast.LENGTH_SHORT).show();
                    personelleriGetir();
                },
                error -> Toast.makeText(this, "Ekleme hatası", Toast.LENGTH_SHORT).show());
        Volley.newRequestQueue(this).add(request);
    }

    private void personelSil(int id) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.DELETE, baseUrl + "/kullanici-sil/" + id, null,
                response -> {
                    Toast.makeText(this, "Personel silindi", Toast.LENGTH_SHORT).show();
                    personelleriGetir();
                },
                error -> Toast.makeText(this, "Silme hatası", Toast.LENGTH_SHORT).show());
        Volley.newRequestQueue(this).add(request);
    }
}