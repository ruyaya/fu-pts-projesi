import cv2
import pytesseract
import numpy as np
import imutils

# 1. Tesseract Yolu
pytesseract.pytesseract.tesseract_cmd = r'C:\Program Files\Tesseract-OCR\tesseract.exe'

# 2. Resim Yükleme ve Hazırlama
resim_adi = "3.jpg"
img = cv2.imread(resim_adi)
if img is None:
    print("HATA: Resim bulunamadı!")
    exit()

img = imutils.resize(img, width=600)
orijinal = img.copy()
gri = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

# 3. MSER ile Karakter Avı
mser = cv2.MSER_create()
regions, _ = mser.detectRegions(gri)

karakter_adaylari = []
for p in regions:
    (x, y, w, h) = cv2.boundingRect(p.reshape(-1, 1, 2))
    if h < 10 or w > 60 or h > 60:
        continue
    karakter_adaylari.append((x, y, w, h))

# 4. Akıllı Gruplama
if not karakter_adaylari:
    print("Hiç karakter adayı bulunamadı.")
else:
    karakter_adaylari = sorted(karakter_adaylari, key=lambda item: item[0])
    olasi_plakalar = []
    
    for i, c1 in enumerate(karakter_adaylari):
        x1, y1, w1, h1 = c1
        grup = [c1]
        for j in range(i + 1, len(karakter_adaylari)):
            c2 = karakter_adaylari[j]
            x2, y2, w2, h2 = c2
            y_farki = abs(y1 - y2)
            x_mesafesi = x2 - (x1 + w1)
            if y_farki < h1 * 0.7 and x_mesafesi < w1 * 2:
                grup.append(c2)
                x1, y1, w1, h1 = c2
        if len(grup) >= 4:
            olasi_plakalar.append(grup)

    if olasi_plakalar:
        olasi_plakalar = sorted(olasi_plakalar, key=len, reverse=True)
        en_iyi_grup = olasi_plakalar[0]
        
        min_x = min([x for (x, y, w, h) in en_iyi_grup])
        max_x = max([x + w for (x, y, w, h) in en_iyi_grup])
        min_y = min([y for (x, y, w, h) in en_iyi_grup])
        max_y = max([y + h for (x, y, w, h) in en_iyi_grup])
        
        oran = (max_x - min_x) / float(max_y - min_y)

        if 2.5 <= oran <= 6.0:
            print("Plaka bulundu ve okuma yapılıyor...")
            
            p_x = max(0, min_x - 5)
            p_y = max(0, min_y - 5)
            p_w = (max_x - min_x) + 10
            p_h = (max_y - min_y) + 10
            
            plaka_resmi = orijinal[p_y:p_y+p_h, p_x:p_x+p_w]
            plaka_gri = cv2.cvtColor(plaka_resmi, cv2.COLOR_BGR2GRAY)
            
            # --- TEK DEĞİŞİKLİK BURADA ---
            # Yazıyı SİYAH, arkaplanı BEYAZ yapıyoruz (Tesseract bunu daha çok sever)
            _, plaka_thresh = cv2.threshold(plaka_gri, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
            # Eskisi: cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU
            # ---------------------------

            config = r'--oem 3 --psm 7 -c tessedit_char_whitelist=ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
            metin = pytesseract.image_to_string(plaka_thresh, lang='tur', config=config)
            temiz_metin = "".join([c for c in metin if c.isalnum()])
            
            print("-" * 30)
            print(f"SONUÇ: {temiz_metin}")
            print("-" * 30)
            
            cv2.rectangle(orijinal, (p_x, p_y), (p_x + p_w, p_y + p_h), (0, 0, 255), 2)
            cv2.imshow("Bulunan Plaka", plaka_resmi)
            cv2.imshow("OCR'a Giden (Beyaz Zemin)", plaka_thresh)
        else:
            print("En iyi grubun oranı plaka oranına uymuyor.")
    else:
        print("Yeterli sayıda karakter içeren bir grup bulunamadı.")

cv2.imshow("Ana Resim", orijinal)
cv2.waitKey(0)
cv2.destroyAllWindows()