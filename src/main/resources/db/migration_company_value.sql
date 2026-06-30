-- =====================================================================
--  Müşteri Değeri -> Şirket Değeri taşıma migration
--  Değer artık şirkete ait; müşteri şirketinin değerini miras alır.
--  Oracle'da ELLE çalıştırılmalıdır.
-- =====================================================================

-- 1) Şirkete değer kolonu ----------------------------------------------
ALTER TABLE Eren_companies ADD musteri_degeri VARCHAR2(20);

-- 2) BACKFILL: mevcut müşterilerin değerini şirketlerine taşı ----------
--    (Bir şirketteki herhangi bir müşterinin değeri şirket değeri olur.)
UPDATE Eren_companies c
SET musteri_degeri = (
    SELECT MAX(u.musteri_degeri)
    FROM Eren_users u
    WHERE u.company_id = c.company_id
      AND u.musteri_degeri IS NOT NULL
)
WHERE EXISTS (
    SELECT 1 FROM Eren_users u
    WHERE u.company_id = c.company_id AND u.musteri_degeri IS NOT NULL
);

-- Değeri boş kalan şirketlere varsayılan ata (opsiyonel):
-- UPDATE Eren_companies SET musteri_degeri = 'ORTA' WHERE musteri_degeri IS NULL;

COMMIT;

-- 3) ESKİ CHECK CONSTRAINT'İ DÜŞÜR (ZORUNLU)
--    musteri_degeri <-> role ilişkisini zorlayan eski kural artık geçersiz;
--    müşteri değeri şirkete taşındı. Bu constraint kalırsa yeni kullanıcı
--    eklerken ORA-02290 hatası alınır.
ALTER TABLE Eren_users DROP CONSTRAINT EREN_CHK_USERS_MST_DGR_ROL;

-- 4) Kullanıcıdaki eski kolon artık kullanılmıyor.
--    Veriyi taşıdıktan SONRA düşürebilirsin (opsiyonel; kolonu düşürürsen
--    yukarıdaki constraint de otomatik kalkar):
-- ALTER TABLE Eren_users DROP COLUMN musteri_degeri;

COMMIT;
