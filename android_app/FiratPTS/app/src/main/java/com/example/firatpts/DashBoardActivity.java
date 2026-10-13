package com.example.firatpts;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DashBoardActivity extends AppCompatActivity {
    private PreviewView canliKamera;
    private TextView okunanPlaka, isimText;
    private FloatingActionButton btnYeniArac;
    private TextRecognizer yapayzeka_motoru;
    private String sonOkunanPlaka = "";
    private int plakaSayaci = 0;
    private final int onay_siniri = 3;
    private boolean plakaKilitlendi = false;
    public static Bitmap sonAracGorseli;
    private LinearLayout bariyerKutusu;
    private BottomNavigationView bottomNavigationView;
    private String sunucuURL = "http://127.0.0.1:5000/api/plaka-sorgula";

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) startCamera();
                else Toast.makeText(this, "Kamera izni şart!", Toast.LENGTH_LONG).show();
            });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash_board);
        canliKamera = findViewById(R.id.canli_kamera);
        okunanPlaka = findViewById(R.id.son_plaka);
        isimText = findViewById(R.id.son_plaka2);
        btnYeniArac = findViewById(R.id.floatingActionButton2);
        yapayzeka_motoru = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bariyerKutusu = findViewById(R.id.bariyer_uyari_kutusu);

        String gelenRol = getIntent().getStringExtra("KULLANICI_ROLU");
        if (bottomNavigationView != null && gelenRol != null) {
            if (gelenRol.equals("gorevli")) {
                MenuItem ekleItem = bottomNavigationView.getMenu().findItem(R.id.arac_ekleme_menu);
                MenuItem duzenleItem = bottomNavigationView.getMenu().findItem(R.id.duzenle_menu);
                if (ekleItem != null) ekleItem.setVisible(false);
                if (duzenleItem != null) duzenleItem.setVisible(false);
            }
        }

        btnYeniArac.setOnClickListener(v -> ekranVeKilidiSifirla());
        bottomNavigationView.setSelectedItemId(R.id.dashboard_menu);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.kayitlar_menu) startActivity(new Intent(this, kayitlar.class));
            else if (itemId == R.id.arac_ekleme_menu) startActivity(new Intent(this, arac_ekleme.class));
            else if (itemId == R.id.duzenle_menu) startActivity(new Intent(this, personelYonetimActivity.class));
            return true;
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }
    private void ekranVeKilidiSifirla() {
        plakaKilitlendi = false;
        plakaSayaci = 0;
        sonOkunanPlaka = "";
        okunanPlaka.setText("TARANIYOR...");
        isimText.setText("-");
        isimText.setTextColor(ContextCompat.getColor(this, android.R.color.white));

        if(bariyerKutusu != null) bariyerKutusu.setVisibility(View.GONE);

        String gelenRol = getIntent().getStringExtra("KULLANICI_ROLU");
        if (bottomNavigationView != null && !"gorevli".equals(gelenRol)) {
            MenuItem ekleItem = bottomNavigationView.getMenu().findItem(R.id.arac_ekleme_menu);
            if (ekleItem != null) ekleItem.setVisible(true);
        }
    }
    @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(canliKamera.getSurfaceProvider());
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
                imageAnalysis.setAnalyzer(java.util.concurrent.Executors.newSingleThreadExecutor(), imageProxy -> {
                    Image mediaImage = imageProxy.getImage();
                    if (mediaImage != null) {
                        InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                        yapayzeka_motoru.process(image)
                                .addOnSuccessListener(visionText -> plakayiFiltreleVeGoster(visionText.getText()))
                                .addOnCompleteListener(task -> imageProxy.close());
                    } else {
                        imageProxy.close();
                    }
                });
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis);
            } catch (Exception e) {
                Log.e("FiratPTS", "Kamera Hatası", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }
    private void plakayiFiltreleVeGoster(String metin) {
        if (plakaKilitlendi) return;
        String temizMetin = metin.toUpperCase().replace("\n", " ").replaceAll("[^A-Z0-9 ]", " ");

        Pattern pattern = Pattern.compile("(([0-9]{2})\\s*)?([A-Z]{1,3})\\s*([0-9]{2,4})");
        Matcher matcher = pattern.matcher(temizMetin);
        while (matcher.find()) {
            String ilKodu = matcher.group(2);
            String harfler = matcher.group(3);
            String rakamlar = matcher.group(4);
            if (harfler == null || rakamlar == null) continue;
            harfler = harfler.trim();
            rakamlar = rakamlar.trim();
            if (harfler.contains("SISTEM") || harfler.contains("AKTIF") || (ilKodu == null && harfler.length() < 2)) {
                continue;
            }
            String şıkPlaka;
            if (ilKodu != null && !ilKodu.trim().isEmpty()) {
                şıkPlaka = ilKodu.trim() + " " + harfler + " " + rakamlar;
            } else {
                şıkPlaka = harfler + " " + rakamlar;
            }
            if (şıkPlaka.equals(sonOkunanPlaka)) {
                plakaSayaci++;
            } else {
                sonOkunanPlaka = şıkPlaka;
                plakaSayaci = 1;
            }
            if (plakaSayaci >= onay_siniri) {
                plakaKilitlendi = true;
                Bitmap anlikGorsel = canliKamera.getBitmap();
                DashBoardActivity.sonAracGorseli = anlikGorsel;

                JSONObject gonderilecekVeri = new JSONObject();
                try {
                    gonderilecekVeri.put("plaka", şıkPlaka);
                    gonderilecekVeri.put("gorsel", bitmapToBase64(anlikGorsel));
                } catch (JSONException e) { e.printStackTrace(); }

                JsonObjectRequest istek = new JsonObjectRequest(Request.Method.POST, sunucuURL, gonderilecekVeri,
                        response -> {
                            try {
                                String gelenIsim = response.getString("isim");
                                String islemSonucu = response.getString("islem_sonucu");
                                String durumKodu = response.getString("durum_kodu");
                                String personelGorevi = response.optString("kategori", "Bilinmiyor");

                                okunanPlaka.setText(şıkPlaka);

                                String gelenRol = getIntent().getStringExtra("KULLANICI_ROLU");
                                MenuItem ekleItem = (bottomNavigationView != null) ?
                                        bottomNavigationView.getMenu().findItem(R.id.arac_ekleme_menu) : null;

                                if ("KIRMIZI_ALARM".equals(durumKodu)) {
                                    isimText.setTextColor(android.graphics.Color.RED);
                                    isimText.setText(gelenIsim + " (" + personelGorevi + ")\n" + islemSonucu);

                                    if (ekleItem != null && !"gorevli".equals(gelenRol)) ekleItem.setVisible(false);

                                    try {
                                        android.media.ToneGenerator toneG = new android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100);
                                        toneG.startTone(android.media.ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 2000);
                                    } catch (Exception e) { e.printStackTrace(); }

                                    android.os.Vibrator v = (android.os.Vibrator) getSystemService(android.content.Context.VIBRATOR_SERVICE);
                                    if (v != null && v.hasVibrator()) {
                                        long[] patternTitre = {0, 400, 200, 400, 200, 400};
                                        v.vibrate(android.os.VibrationEffect.createWaveform(patternTitre, -1));
                                    }
                                    Toast.makeText(this, "⚠️ GÜVENLİK İHLALİ!", Toast.LENGTH_LONG).show();

                                    new android.os.Handler().postDelayed(this::ekranVeKilidiSifirla, 3000);

                                } else if ("YABANCI".equals(durumKodu)) {
                                    if (ekleItem != null && !"gorevli".equals(gelenRol)) ekleItem.setVisible(true);

                                    Intent intent = new Intent(DashBoardActivity.this, arac_ekleme.class);
                                    intent.putExtra("PLAKA_NO", şıkPlaka);
                                    startActivity(intent);
                                } else {
                                    if (ekleItem != null && !"gorevli".equals(gelenRol)) ekleItem.setVisible(false);

                                    isimText.setTextColor(android.graphics.Color.GREEN);
                                    isimText.setText(gelenIsim + " - " + personelGorevi + "\n(" + islemSonucu + ")");

                                    if(bariyerKutusu != null) bariyerKutusu.setVisibility(View.VISIBLE);

                                    try {
                                        android.media.ToneGenerator toneG = new android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100);
                                        toneG.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 500);
                                    } catch (Exception e) { e.printStackTrace(); }

                                    new android.os.Handler().postDelayed(this::ekranVeKilidiSifirla, 3500);
                                }

                            } catch (Exception e) {
                                android.util.Log.e("FiratPTS", "JSON Hatası: " + e.getMessage());
                                ekranVeKilidiSifirla();
                            }
                        },
                        error -> {
                            Log.e("FiratPTS", "Sunucu Hatası");
                            okunanPlaka.setText("BAĞLANTI HATASI");
                            isimText.setText("Sunucuya ulaşılamadı");
                            isimText.setTextColor(android.graphics.Color.RED);
                            ekranVeKilidiSifirla();
                        }
                ) {
                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("ngrok-skip-browser-warning", "true");
                        return headers;
                    }
                };
                istek.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(10000, 1, 1.0f));
                Volley.newRequestQueue(this).add(istek);
            }
            return;
        }
    }
    private String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return "";
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
    }
    @Override
    protected void onResume() {
        super.onResume();
        ekranVeKilidiSifirla();
        if (btnYeniArac != null) {
            btnYeniArac.setOnClickListener(v -> {
                ekranVeKilidiSifirla();
                Toast.makeText(this, "Sistem Sıfırlandı, Taramaya Hazır!", Toast.LENGTH_SHORT).show();
            });
        }
    }
}