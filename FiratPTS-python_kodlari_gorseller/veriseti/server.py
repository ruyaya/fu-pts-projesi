from flask import Flask, jsonify, request
from werkzeug.security import generate_password_hash, check_password_hash
import sqlite3
import base64
import os
from datetime import datetime
import pytz 

app = Flask(__name__)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DB_PATH = os.path.join(BASE_DIR, 'plaka_sistemi.db')

def get_db_connection():
    conn = sqlite3.connect(DB_PATH, check_same_thread=False)
    conn.row_factory = sqlite3.Row 
    return conn

def veritabanini_hazirla():
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS kullanicilar (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            kullanici_adi TEXT UNIQUE NOT NULL,
            sifre_hash TEXT NOT NULL,
            rol TEXT NOT NULL
        )
    ''')

    cursor.execute('''
        CREATE TABLE IF NOT EXISTS araclar (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            plaka TEXT UNIQUE NOT NULL,
            sahip TEXT NOT NULL,
            kategori TEXT NOT NULL, 
            gecis_izni TEXT NOT NULL, 
            kayit_tarihi TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')

    cursor.execute('''
        CREATE TABLE IF NOT EXISTS gecis_kayitlari (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            plaka TEXT NOT NULL,
            gecis_saati TIMESTAMP,
            islem_sonucu TEXT NOT NULL, 
            fotograf_yolu TEXT NOT NULL
        )
    ''')

    cursor.execute("SELECT COUNT(*) FROM kullanicilar")
    if cursor.fetchone()[0] == 0:
        yonetici_hash = generate_password_hash('admin123')
        gorevli_hash = generate_password_hash('firat23')
        cursor.execute("INSERT INTO kullanicilar (kullanici_adi, sifre_hash, rol) VALUES (?, ?, ?)",
                       ('yonetici', yonetici_hash, 'admin'))
        cursor.execute("INSERT INTO kullanicilar (kullanici_adi, sifre_hash, rol) VALUES (?, ?, ?)",
                       ('guvenlik1', gorevli_hash, 'gorevli'))
        
    conn.commit()
    conn.close()

veritabanini_hazirla()


@app.route('/api/giris', methods=['POST'])
def giris_yap():
    veri = request.get_json()
    gelen_kullanici = veri.get('kullanici_adi')
    gelen_sifre = veri.get('sifre')

    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM kullanicilar WHERE kullanici_adi = ?", (gelen_kullanici,))
    kullanici = cursor.fetchone()
    conn.close()

    if kullanici and check_password_hash(kullanici['sifre_hash'], gelen_sifre):
        return jsonify({"durum": "basarili", "rol": kullanici['rol'], "mesaj": "Giriş Onaylandı"})
    else:
        return jsonify({"durum": "hata", "mesaj": "Hatalı kullanıcı adı veya şifre!"}), 401

@app.route('/api/araclar', methods=['GET'])
def araclari_getir():
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM araclar")
    araclar_listesi = cursor.fetchall()
    conn.close()

    sonuc = []
    for arac in araclar_listesi:
        sonuc.append({
            "id": arac['id'],
            "plaka": arac['plaka'],
            "sahip": arac['sahip'],
            "kategori": arac['kategori'],    
            "gecis_izni": arac['gecis_izni']  
        })
    return jsonify(sonuc)


UPLOAD_FOLDER = os.path.join(BASE_DIR, 'gecis_fotograflari')
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)

@app.route('/api/plaka-sorgula', methods=['POST'])
def plaka_sorgula_api():
    gelen_veri = request.get_json()
    okunan_plaka = gelen_veri.get('plaka', '')
    gorsel_base64 = gelen_veri.get('gorsel', '') 

    tz_tr = pytz.timezone('Europe/Istanbul')
    anlik_zaman = datetime.now(tz_tr)
    dogru_saat = anlik_zaman.strftime('%Y-%m-%d %H:%M:%S')

    dosya_adi = f"{okunan_plaka}_{anlik_zaman.strftime('%Y%m%d_%H%M%S')}.jpg"
    dosya_yolu = os.path.join(UPLOAD_FOLDER, dosya_adi)
    
    with open(dosya_yolu, "wb") as f:
        f.write(base64.b64decode(gorsel_base64))

    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM araclar WHERE plaka = ?", (okunan_plaka,))
    kayitli_arac = cursor.fetchone()

    islem_sonucu = "BİLİNMİYOR"
    durum_kodu = "YABANCI" 

    if kayitli_arac:
        isim = kayitli_arac['sahip']
        kategori = kayitli_arac['kategori']
        gecis_izni = kayitli_arac['gecis_izni']

        if gecis_izni == "YASAKLI":
            islem_sonucu = "YASAKLI ARAÇ ALARMI"
            durum_kodu = "KIRMIZI_ALARM"
        else:
            islem_sonucu = "GİRİŞ ONAYLANDI"
            durum_kodu = "YESIL_ISIK"
    else:
        isim = "Sistemde Yok"
        kategori = "Yabancı Araç"
        islem_sonucu = "KAYITSIZ ARAÇ TESPİTİ"
        durum_kodu = "YABANCI"

    cursor.execute("INSERT INTO gecis_kayitlari (plaka, gecis_saati, islem_sonucu, fotograf_yolu) VALUES (?, ?, ?, ?)", 
                   (okunan_plaka, dogru_saat, islem_sonucu, dosya_yolu))
    conn.commit()
    conn.close()

    return jsonify({
        "isim": isim,
        "kategori": kategori,
        "islem_sonucu": islem_sonucu,
        "durum_kodu": durum_kodu,
        "gecis_saati": dogru_saat
    })


@app.route('/api/kullanicilar', methods=['GET'])
def kullanicilari_getir():
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT id, kullanici_adi, rol FROM kullanicilar WHERE rol = 'gorevli'")
    personeller = cursor.fetchall()
    conn.close()

    liste = []
    for p in personeller:
        liste.append({"id": p['id'], "kullanici_adi": p['kullanici_adi'], "rol": p['rol']})
    return jsonify(liste)

@app.route('/api/kullanici-ekle', methods=['POST'])
def kullanici_ekle():
    veri = request.get_json()
    k_adi = veri.get('kullanici_adi')
    sifre = veri.get('sifre')
    
    if not k_adi or not sifre:
        return jsonify({"durum": "hata", "mesaj": "Bilgiler eksik"}), 400

    sifre_hash = generate_password_hash(sifre)
    conn = get_db_connection()
    cursor = conn.cursor()
    try:
        cursor.execute("INSERT INTO kullanicilar (kullanici_adi, sifre_hash, rol) VALUES (?, ?, ?)",
                       (k_adi, sifre_hash, 'gorevli'))
        conn.commit()
        return jsonify({"durum": "basarili", "mesaj": "Yeni görevli eklendi"})
    except sqlite3.IntegrityError:
        return jsonify({"durum": "hata", "mesaj": "Bu isimde biri zaten var!"}), 400
    finally:
        conn.close()

@app.route('/api/kullanici-sil/<int:user_id>', methods=['DELETE'])
def kullanici_sil(user_id):
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("DELETE FROM kullanicilar WHERE id = ? AND rol != 'admin'", (user_id,))
    conn.commit()
    conn.close()
    return jsonify({"durum": "basarili", "mesaj": "Görevli silindi"})


@app.route('/api/arac-ekle', methods=['POST'])
def arac_ekle():
    veri = request.get_json()
    plaka = veri.get('plaka')
    sahip = veri.get('sahip')
    kategori = veri.get('durum') 
    gecis_izni = veri.get('gecis_izni', 'ONAYLI') 

    conn = get_db_connection()
    cursor = conn.cursor()
    try:
        cursor.execute("INSERT INTO araclar (plaka, sahip, kategori, gecis_izni) VALUES (?, ?, ?, ?)",
                       (plaka, sahip, kategori, gecis_izni))
        conn.commit()
        return jsonify({"durum": "basarili", "mesaj": "Araç sisteme yetkilendirildi"})
    except sqlite3.IntegrityError:
        return jsonify({"durum": "hata", "mesaj": "Bu plaka zaten sistemde var!"}), 400
    except Exception as e:
        return jsonify({"durum": "hata", "mesaj": "Veritabanı hatası"}), 500
    finally:
        conn.close()


@app.route('/api/kayitlar', methods=['GET'])
def kayitlari_getir():
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("""
        SELECT 
            g.plaka, 
            g.gecis_saati, 
            g.islem_sonucu, 
            g.fotograf_yolu, 
            a.sahip, 
            a.kategori 
        FROM gecis_kayitlari g 
        LEFT JOIN araclar a ON g.plaka = a.plaka 
        ORDER BY g.gecis_saati DESC
    """)
    kayitlar = cursor.fetchall()
    conn.close()

    liste = []
    for k in kayitlar: 
        gorsel_data = ""
        fotograf_yolu = k[3] # Index 3: g.fotograf_yolu
        
        if fotograf_yolu and os.path.exists(fotograf_yolu):
            try:
                with open(fotograf_yolu, "rb") as img_file:
                    gorsel_data = base64.b64encode(img_file.read()).decode('utf-8')
            except Exception:
                pass

        sahip = k[4] if k[4] else "Bilinmiyor" 
        kategori = k[5] if k[5] else "Kayıtsız Araç"

        liste.append({
            "plaka": k[0],
            "saat": k[1],
            "sonuc": k[2],
            "sahip": sahip,
            "kategori": kategori,
            "gorsel": gorsel_data
        })
    return jsonify(liste)


if __name__ == '__main__': 
    app.run(host='0.0.0.0', port=5000, debug=True)