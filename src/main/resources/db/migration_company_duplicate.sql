-- =====================================================================
--  Company + Talep Birleştirme (Duplicate Merge) migration
--  Oracle'da ELLE çalıştırılmalıdır. Uygulama tabloları otomatik
--  oluşturmaz (DataInitializer yalnızca satır ekler).
-- =====================================================================

-- 1) ŞİRKET tablosu ----------------------------------------------------
CREATE TABLE Eren_companies (
    company_id  NUMBER PRIMARY KEY,
    name        VARCHAR2(150) NOT NULL,
    created_at  TIMESTAMP DEFAULT SYSTIMESTAMP,
    updated_at  TIMESTAMP DEFAULT SYSTIMESTAMP
);

CREATE SEQUENCE Eren_seq_companies START WITH 1 INCREMENT BY 1 NOCACHE;

-- 2) Kullanıcı -> Şirket bağı ------------------------------------------
ALTER TABLE Eren_users ADD company_id NUMBER;
ALTER TABLE Eren_users ADD CONSTRAINT fk_users_company
    FOREIGN KEY (company_id) REFERENCES Eren_companies(company_id);

-- 3) Talep birleştirme alanı -------------------------------------------
ALTER TABLE Eren_requests ADD merged_into NUMBER;
ALTER TABLE Eren_requests ADD CONSTRAINT fk_requests_merged
    FOREIGN KEY (merged_into) REFERENCES Eren_requests(request_id);

-- 4) status CHECK constraint'ine DUPLICATE değerini ekle (ZORUNLU).
--    Bu yapılmazsa "Birleştir" işleminde ORA-02290 alınır.
--    Eğer tabloda aşağıdaki 5 değerin dışında statü taşıyan kayıt varsa
--    (SELECT DISTINCT status FROM Eren_requests;) onları da listeye ekle.
ALTER TABLE Eren_requests DROP CONSTRAINT EREN_CHK_REQUESTS_STATUS;
ALTER TABLE Eren_requests ADD CONSTRAINT EREN_CHK_REQUESTS_STATUS
  CHECK (status IN ('NEW','UNDER_REVIEW','PRIORITIZED','REJECTED','DUPLICATE'));
COMMIT;

-- =====================================================================
--  MEVCUT VERİ İÇİN BACKFILL (DB'de zaten kullanıcılar varsa)
--  DataInitializer kullanıcı varken atlandığı için şirketleri ve
--  atamaları aşağıdaki gibi elle eklemen gerekir. (Demo amaçlı.)
-- =====================================================================
-- INSERT INTO Eren_companies (company_id, name) VALUES (Eren_seq_companies.NEXTVAL, 'TeknoCorp');
-- INSERT INTO Eren_companies (company_id, name) VALUES (Eren_seq_companies.NEXTVAL, 'Global A.Ş.');
--
-- -- Aynı şirkete 2 müşteri örneği (TeknoCorp):
-- UPDATE Eren_users SET company_id = (SELECT company_id FROM Eren_companies WHERE name='TeknoCorp')
--   WHERE email IN ('yetkili@teknocorp.com');
-- UPDATE Eren_users SET company_id = (SELECT company_id FROM Eren_companies WHERE name='Global A.Ş.')
--   WHERE email IN ('temsilci@globalas.com');
-- COMMIT;
