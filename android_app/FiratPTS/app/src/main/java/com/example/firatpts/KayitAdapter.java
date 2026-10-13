package com.example.firatpts;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;

public class KayitAdapter extends RecyclerView.Adapter<KayitAdapter.ViewHolder> {
    private JSONArray kayitlar;

    public KayitAdapter(JSONArray kayitlar) { this.kayitlar = kayitlar; }

    public void listeyiGuncelle(JSONArray yeniListe) {
        this.kayitlar = yeniListe;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_kayit, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            JSONObject obj = kayitlar.getJSONObject(position);

            String plaka = obj.optString("plaka", "");
            String saat = obj.optString("saat", "");
            String sahip = obj.optString("sahip", "Bilinmiyor");
            String kategori = obj.optString("kategori", "");
            String sonucStr = obj.optString("sonuc", "");
            String gorselBase64 = obj.optString("gorsel", "");

            holder.txtPlaka.setText(plaka);
            holder.txtTarih.setText(saat);
            holder.txtSahipGorev.setText(sahip + " (" + kategori + ")");
            holder.txtSonuc.setText(sonucStr);
            if(sonucStr.contains("YASAKLI") || sonucStr.contains("KAYITSIZ")) {
                holder.txtSonuc.setTextColor(android.graphics.Color.RED);
            } else {
                holder.txtSonuc.setTextColor(android.graphics.Color.GREEN);
            }
            if (!gorselBase64.isEmpty()) {
                new Thread(() -> {
                    try {
                        byte[] decodedString = Base64.decode(gorselBase64, Base64.DEFAULT);
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 2; // RAM'i şişirmemek için fotoğrafı optimize eder
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length, options);
                        holder.imgArac.post(() -> holder.imgArac.setImageBitmap(decodedByte));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                holder.imgArac.setImageResource(android.R.drawable.ic_menu_camera);
            }
            holder.itemView.setOnClickListener(v -> {
                Context context = v.getContext();
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                LinearLayout layout = new LinearLayout(context);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(50, 40, 50, 40);
                layout.setGravity(Gravity.CENTER);

                ImageView dialogGorsel = new ImageView(context);
                dialogGorsel.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        600));
                dialogGorsel.setScaleType(ImageView.ScaleType.CENTER_CROP);

                if (!gorselBase64.isEmpty()) {
                    byte[] decodedString = Base64.decode(gorselBase64, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    dialogGorsel.setImageBitmap(decodedByte);
                } else {
                    dialogGorsel.setImageResource(android.R.drawable.ic_menu_camera);
                }
                layout.addView(dialogGorsel);

                TextView detayMetni = new TextView(context);
                detayMetni.setTextSize(16f);
                detayMetni.setLineSpacing(0, 1.4f);
                detayMetni.setPadding(0, 40, 0, 0);

                String detaylar = "📋 Plaka: " + plaka +
                        "\n👤 Sahip: " + sahip +
                        "\n🏷️ Görev: " + kategori +
                        "\n🕒 Geçiş Saati: " + saat +
                        "\n🛑 Sonuç: " + sonucStr;

                detayMetni.setText(detaylar);
                layout.addView(detayMetni);

                builder.setView(layout);
                builder.setTitle("Araç Geçiş Detayları");
                builder.setPositiveButton("KAPAT", (dialog, which) -> dialog.dismiss());

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            });

        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override public int getItemCount() { return kayitlar != null ? kayitlar.length() : 0; }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtPlaka, txtTarih, txtSonuc, txtSahipGorev;
        ImageView imgArac;
        public ViewHolder(View v) {
            super(v);
            txtPlaka = v.findViewById(R.id.txtPlaka);
            txtTarih = v.findViewById(R.id.txtTarih);
            txtSonuc = v.findViewById(R.id.txtSonuc);
            txtSahipGorev = v.findViewById(R.id.txtSahipGorev);
            imgArac = v.findViewById(R.id.imgArac);
        }
    }
}