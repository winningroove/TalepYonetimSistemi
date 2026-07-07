package com.example.prioritization;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Önceliklendirme skoru için ayarlanabilir katsayılar ve eşikler.
 * <p>
 * Varsayılan değerler burada tanımlıdır; istenirse {@code application.properties}
 * içinde {@code prioritization.*} anahtarlarıyla koda dokunmadan değiştirilebilir.
 * Böylece iş kuralı sabitleri (ağırlıklar, etiket eşikleri, güvenilirlik kuralları)
 * hard-code edilmez.
 */
@Component
@ConfigurationProperties(prefix = "prioritization")
@Data
public class PrioritizationProperties {

    private Agirlik agirlik = new Agirlik();
    private Etiket etiket = new Etiket();
    private Guvenilirlik guvenilirlik = new Guvenilirlik();
    private Bekleme bekleme = new Bekleme();

    /** Baz skor faktör ağırlıkları. Ağırlıklı toplam {@code bolen}'e bölünür. */
    @Data
    public static class Agirlik {
        private int isEtkisi = 30;
        private int aciliyet = 25;
        private int musteriDegeri = 20;
        private int isTipi = 15;
        private int beklemeSuresi = 10;
        private double bolen = 5.0;
    }

    /** Skor → etiket eşikleri (üst sınırlar). 0 = İPTAL; üstü KRİTİK. */
    @Data
    public static class Etiket {
        private int cokDusuk = 36;
        private int dusuk = 52;
        private int orta = 68;
        private int yuksek = 84;
    }

    /** Talep sahibinin geçmiş performansına göre ödül/ceza. */
    @Data
    public static class Guvenilirlik {
        private int minTalep = 5;            // en az bu kadar talep yoksa skor 0
        private double redOraniEsik = 0.70;  // reddedilme oranı bunu aşarsa ceza
        private double onayOraniEsik = 0.80; // onaylanma oranı bunu geçerse ödül
        private int ceza = -10;
        private int odul = 5;
    }

    /**
     * Talebin bekleme süresine (gün) göre verilen puan kademeleri.
     * Puanlar 1–5 aralığındadır: diğer faktörlerle aynı skala ve DB check
     * constraint'i (EREN_CHK_PRIOR_BEKLEME_PUAN) ile uyumlu; skor formülünün
     * 0–100 kalibrasyonunu korur.
     */
    @Data
    public static class Bekleme {
        private int maxGun = 30;
        private int cokUzunGun = 30; private int cokUzunPuan = 5;
        private int uzunGun = 14;    private int uzunPuan = 4;
        private int ortaGun = 7;     private int ortaPuan = 3;
        private int kisaGun = 3;     private int kisaPuan = 2;
        private int varsayilanPuan = 1;
    }
}
