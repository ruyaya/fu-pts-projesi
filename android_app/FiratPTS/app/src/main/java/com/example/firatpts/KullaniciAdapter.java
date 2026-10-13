package com.example.firatpts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class KullaniciAdapter extends RecyclerView.Adapter<KullaniciAdapter.ViewHolder> {
    private List<Kullanici> kullaniciListesi;
    private OnSilClickListener listener;

    public interface OnSilClickListener {
        void onSilClick(int id);
    }

    public KullaniciAdapter(List<Kullanici> kullaniciListesi, OnSilClickListener listener) {
        this.kullaniciListesi = kullaniciListesi;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_personel, parent, false);
        return new ViewHolder(v);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Kullanici k = kullaniciListesi.get(position);
        holder.tvAd.setText(k.getKullaniciAdi());

        if ("gorevli".equalsIgnoreCase(k.getRol())) {
            holder.tvRol.setText("🛡️ Kampüs Güvenlik Görevlisi");
        } else {
            holder.tvRol.setText("⚙️ Sistem Yöneticisi");
        }

        holder.btnSil.setOnClickListener(v -> listener.onSilClick(k.getId()));
    }
    @Override
    public int getItemCount() { return kullaniciListesi.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAd, tvRol;
        ImageButton btnSil;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAd = itemView.findViewById(R.id.tvGorevliAdi);
            tvRol = itemView.findViewById(R.id.tvGorevliRol);
            btnSil = itemView.findViewById(R.id.btnGorevliSil);
        }
    }
}