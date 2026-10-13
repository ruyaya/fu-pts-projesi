package com.example.firatpts;

public class Kullanici {
    private int id;
    private String kullaniciAdi;
    private String rol;

    public Kullanici(int id, String kullaniciAdi, String rol) {
        this.id = id;
        this.kullaniciAdi = kullaniciAdi;
        this.rol = rol;
    }

    public int getId() { return id; }
    public String getKullaniciAdi() { return kullaniciAdi; }
    public String getRol() { return rol; }
}