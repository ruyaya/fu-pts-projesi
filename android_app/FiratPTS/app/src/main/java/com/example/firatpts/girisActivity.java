package com.example.firatpts;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class girisActivity extends AppCompatActivity {

    private EditText editKullanici, editSifre;
    private Button btnGiris;
    private String girisurl = "http://127.0.0.1:5000/api/giris";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_giris);
        editKullanici = findViewById(R.id.editKullaniciAdi);
        editSifre = findViewById(R.id.editSifre);
        btnGiris = findViewById(R.id.btnGirisYap);

        btnGiris.setOnClickListener(v -> {
            String kullanici = editKullanici.getText().toString().trim();
            String sifre = editSifre.getText().toString().trim();

            if (!kullanici.isEmpty() && !sifre.isEmpty()) {
                girisYap(kullanici, sifre);
            } else {
                Toast.makeText(this, "Lütfen tüm alanları doldurun!", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void girisYap(String kAdi, String sifre) {
        JSONObject parameters = new JSONObject();
        try {
            parameters.put("kullanici_adi", kAdi);
            parameters.put("sifre", sifre);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, girisurl, parameters,
                response -> {
                    try {
                        if (response.getString("durum").equals("basarili")) {
                            String rol = response.getString("rol");

                            Intent intent = new Intent(girisActivity.this, DashBoardActivity.class);
                            intent.putExtra("KULLANICI_ROLU", rol);
                            startActivity(intent);
                            finish();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    Toast.makeText(this, "Bağlantı veya Giriş Hatası!", Toast.LENGTH_SHORT).show();
                    android.util.Log.e("VolleyHatasi", "Hata detayı: " + error.toString());
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("ngrok-skip-browser-warning", "true");
                return headers;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }
}