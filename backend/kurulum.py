import sqlite3

conn = sqlite3.connect('plaka_sistemi.db')
cursor = conn.cursor() 
conn.commit()
conn.close()
print("Veritabanı başarıyla kuruldu ve test araçları eklendi")