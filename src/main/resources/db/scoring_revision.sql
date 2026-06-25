-- =====================================================================
--  Önceliklendirme Algoritması Revizyonu — Şema Güncellemeleri
--  Hedef tablolar: Eren_requests, Eren_prioritizations
--  JdbcTemplate ile uyumlu; uygulama bu kolonlara isimle erişir.
-- =====================================================================

-- 1) Eren_requests tablosuna sözel "Yönetici Takdiri" kolonu eklenir.
--    Varsayılan 'YOK' (puan karşılığı 0). NOT NULL ki skor hesabı güvenli olsun.
ALTER TABLE Eren_requests
    ADD (yonetici_takdiri VARCHAR2(30) DEFAULT 'YOK' NOT NULL);

-- 2) Mevcut satırlar DEFAULT ile dolar; garanti için NULL kalanları sabitle.
UPDATE Eren_requests SET yonetici_takdiri = 'YOK' WHERE yonetici_takdiri IS NULL;
COMMIT;

-- 3) Yalnızca geçerli sözel değerlere izin veren CHECK kısıtı.
ALTER TABLE Eren_requests
    ADD CONSTRAINT chk_eren_yonetici_takdiri
    CHECK (yonetici_takdiri IN ('YOK', 'ONEMLI', 'STRATEJIK', 'KRITIK'));

-- 4) Eski çarpan (multiplier) mantığı tamamen kaldırıldığı için
--    Eren_prioritizations.yonetici_mudahalesi kolonu artık kullanılmıyor.
ALTER TABLE Eren_prioritizations DROP COLUMN yonetici_mudahalesi;
