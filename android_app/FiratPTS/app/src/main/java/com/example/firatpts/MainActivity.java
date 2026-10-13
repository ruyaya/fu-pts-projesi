package com.example.firatpts;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText girilen_kullaniciAdi = findViewById(R.id.kullanici_adi);
        EditText girilen_sifre = findViewById(R.id.sifre);
        Button girisButonu = findViewById(R.id.btn_giris);

        girisButonu.setOnClickListener(v -> {

            String kullaniciAdi = girilen_kullaniciAdi.getText().toString().trim();
            String sifre = girilen_sifre.getText().toString().trim();

            if (kullaniciAdi.equalsIgnoreCase("ruya") && sifre.equals("123456")) {
                Toast.makeText(MainActivity.this, "Giriş Başarılı", Toast.LENGTH_SHORT).show();

                Intent anaPaneleGec = new Intent(MainActivity.this, DashBoardActivity.class);
                startActivity(anaPaneleGec);
                finish();

            } else {
                Toast.makeText(MainActivity.this, "Hatalı Giriş Yaptınız!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}