package com.example.firatpts;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;

public class kayitlar extends AppCompatActivity {

    private RecyclerView rv;
    private JSONArray globalKayitlar;

    // YENİ EKLENENLER: Filtre menüsü ve güncellenebilir adapter
    private KayitAdapter adapter;
    private Spinner spnFiltre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_kayitlar);

        if (findViewById(R.id.main) != null) {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        rv = findViewById(R.id.recyclerViewKayitlar);
        rv.setLayoutManager(new LinearLayoutManager(this));

        TextView btnPdf = findViewById(R.id.btnPdfRapor);
        if(btnPdf != null) {
            btnPdf.setOnClickListener(v -> pdfOlustur());
        }

        TextView btnKayitliAraclarPdf = findViewById(R.id.btnKayitliAraclarPdf);
        if(btnKayitliAraclarPdf != null) {
            btnKayitliAraclarPdf.setOnClickListener(v -> kayitliAraclariIndir());
        }

        // --- YENİ EKLENEN: FİLTRE MENÜSÜ KURULUMU ---
        spnFiltre = findViewById(R.id.spnFiltre);
        if (spnFiltre != null) {
            String[] filtreSecenekleri = {"Tüm Kayıtlar", "Akademik Personel", "İdari Personel", "Öğrenci", "Misafir", "Sadece Yasaklılar", "Sadece Kayıtsızlar"};

            // Siyah temaya uygun yazı ayarı
            ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filtreSecenekleri);
            spnFiltre.setAdapter(spinAdapter);

            spnFiltre.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if(view != null) ((TextView) view).setTextColor(android.graphics.Color.WHITE);
                    // Veriler indiyse filtreyi çalıştır
                    if (globalKayitlar != null) {
                        filtreyiUygula(filtreSecenekleri[position]);
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        verileriGetir();
    }
    private void verileriGetir() {
        String url = "http://127.0.0.1:5000/api/kayitlar";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    globalKayitlar = response;
                    adapter = new KayitAdapter(response);
                    rv.setAdapter(adapter);
                },
                error -> {
                    String mesaj = "Bağlantı Hatası!";
                    if (error instanceof com.android.volley.TimeoutError) {
                        mesaj = "Sunucu yanıt vermedi (Zaman Aşımı)! Lütfen sunucuyu kontrol edin.";
                    } else if (error.networkResponse != null) {
                        mesaj = "Sunucu Hatası: " + error.networkResponse.statusCode;
                    }
                    Toast.makeText(this, mesaj, Toast.LENGTH_LONG).show();
                }
        );

        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                10000,
                1,
                1.0f));

        Volley.newRequestQueue(this).add(request);
    }

    private void filtreyiUygula(String secilen) {
        if (adapter == null || globalKayitlar == null) return;

        if (secilen.equals("Tüm Kayıtlar")) {
            adapter.listeyiGuncelle(globalKayitlar);
            return;
        }

        JSONArray filtrelenmisListe = new JSONArray();
        try {
            for (int i = 0; i < globalKayitlar.length(); i++) {
                JSONObject obj = globalKayitlar.getJSONObject(i);
                String kategori = obj.optString("kategori", "").toUpperCase();
                String sonuc = obj.optString("sonuc", "").toUpperCase();

                if (secilen.equals("Sadece Yasaklılar") && sonuc.contains("YASAKLI")) {
                    filtrelenmisListe.put(obj);
                } else if (secilen.equals("Sadece Kayıtsızlar") && (sonuc.contains("KAYITSIZ") || kategori.contains("KAYITSIZ"))) {
                    filtrelenmisListe.put(obj);
                } else if (kategori.equalsIgnoreCase(secilen)) {
                    filtrelenmisListe.put(obj);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        adapter.listeyiGuncelle(filtrelenmisListe);
    }

    private void pdfOlustur() {
        if (globalKayitlar == null || globalKayitlar.length() == 0) {
            Toast.makeText(this, "Raporlanacak veri bulunamadı!", Toast.LENGTH_SHORT).show();
            return;
        }

        android.graphics.pdf.PdfDocument document = new android.graphics.pdf.PdfDocument();
        android.graphics.pdf.PdfDocument.PageInfo pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4
        android.graphics.pdf.PdfDocument.Page page = document.startPage(pageInfo);

        android.graphics.Canvas canvas = page.getCanvas();
        android.graphics.Paint paint = new android.graphics.Paint();

        paint.setTextSize(20f);
        paint.setFakeBoldText(true);
        canvas.drawText("FIRAT ÜNİVERSİTESİ PTS GEÇİŞ RAPORU", 50, 50, paint);

        paint.setTextSize(12f);
        paint.setFakeBoldText(false);
        canvas.drawText("Rapor Tarihi: " + new java.util.Date().toString(), 50, 80, paint);

        int y = 120;
        paint.setTextSize(10f);

        try {
            for (int i = 0; i < globalKayitlar.length(); i++) {
                JSONObject kayit = globalKayitlar.getJSONObject(i);
                String satir = (i+1) + ". Plaka: " + kayit.getString("plaka") +
                        " | Saat: " + kayit.getString("saat") +
                        " | Sonuç: " + kayit.getString("sonuc");

                canvas.drawText(satir, 50, y, paint);
                y += 25;
                if (y > 800) break;
            }
        } catch (Exception e) { e.printStackTrace(); }

        document.finishPage(page);
        kaydetVeUyar(document, "FiratPTS_GecisRaporu_");
    }

    private void kayitliAraclariIndir() {
        String url = "http://127.0.0.1:5000/api/araclar";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> pdfOlusturKayitliAraclar(response),
                error -> Toast.makeText(this, "Araç listesi alınamadı: " + error.getMessage(), Toast.LENGTH_SHORT).show()
        );
        Volley.newRequestQueue(this).add(request);
    }

    private void pdfOlusturKayitliAraclar(JSONArray aracListesi) {
        if (aracListesi == null || aracListesi.length() == 0) {
            Toast.makeText(this, "Sistemde kayıtlı araç yok!", Toast.LENGTH_SHORT).show();
            return;
        }

        android.graphics.pdf.PdfDocument document = new android.graphics.pdf.PdfDocument();
        android.graphics.pdf.PdfDocument.PageInfo pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create();
        android.graphics.pdf.PdfDocument.Page page = document.startPage(pageInfo);

        android.graphics.Canvas canvas = page.getCanvas();
        android.graphics.Paint paint = new android.graphics.Paint();

        paint.setTextSize(18f);
        paint.setFakeBoldText(true);
        canvas.drawText("FIRAT PTS - SİSTEME KAYITLI ARAÇLAR", 50, 50, paint);

        int y = 100;
        paint.setTextSize(12f);
        paint.setFakeBoldText(false);

        try {
            for (int i = 0; i < aracListesi.length(); i++) {
                JSONObject arac = aracListesi.getJSONObject(i);
                String plaka = arac.optString("plaka", "-");
                String sahip = arac.optString("sahip", "-");
                String kategori = arac.optString("kategori", "-");
                String izin = arac.optString("gecis_izni", "-");

                String satir = (i+1) + ". Plaka: " + plaka + " | Sahip: " + sahip + " | Görev: " + kategori + " | Durum: " + izin;

                canvas.drawText(satir, 50, y, paint);
                y += 25;

                if (y > 800) {
                    canvas.drawText("... (Liste diğer sayfaya taşıyor) ...", 50, y, paint);
                    break;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        document.finishPage(page);
        kaydetVeUyar(document, "FiratPTS_KayitliAraclar_");
    }

    private void kaydetVeUyar(android.graphics.pdf.PdfDocument document, String dosyaOnEki) {
        try {
            java.io.OutputStream out;
            String dosyaAdi = dosyaOnEki + System.currentTimeMillis() + ".pdf";

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, dosyaAdi);
                values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS);

                android.net.Uri uri = getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                out = getContentResolver().openOutputStream(uri);
            } else {
                java.io.File path = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                java.io.File file = new java.io.File(path, dosyaAdi);
                out = new java.io.FileOutputStream(file);
            }

            document.writeTo(out);
            out.close();
            Toast.makeText(this, "PDF İndirilenler Klasörüne Kaydedildi!", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "HATA: PDF kaydedilemedi!", Toast.LENGTH_SHORT).show();
        }
        document.close();
    }
}