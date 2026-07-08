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

    /**
     * Derin kopya üretir. Config servisi, varsayılanları (bu bean) bozmadan
     * üstüne DB'deki override değerlerini uygulamak için kullanır.
     */
    public PrioritizationProperties copy() {
        PrioritizationProperties c = new PrioritizationProperties();
        Agirlik a = new Agirlik();
        a.setIsEtkisi(agirlik.getIsEtkisi());
        a.setAciliyet(agirlik.getAciliyet());
        a.setMusteriDegeri(agirlik.getMusteriDegeri());
        a.setIsTipi(agirlik.getIsTipi());
        a.setBeklemeSuresi(agirlik.getBeklemeSuresi());
        a.setBolen(agirlik.getBolen());
        c.setAgirlik(a);

        Etiket e = new Etiket();
        e.setCokDusuk(etiket.getCokDusuk());
        e.setDusuk(etiket.getDusuk());
        e.setOrta(etiket.getOrta());
        e.setYuksek(etiket.getYuksek());
        c.setEtiket(e);

        Guvenilirlik g = new Guvenilirlik();
        g.setMinTalep(guvenilirlik.getMinTalep());
        g.setRedOraniEsik(guvenilirlik.getRedOraniEsik());
        g.setOnayOraniEsik(guvenilirlik.getOnayOraniEsik());
        g.setCeza(guvenilirlik.getCeza());
        g.setOdul(guvenilirlik.getOdul());
        c.setGuvenilirlik(g);

        Bekleme b = new Bekleme();
        b.setMaxGun(bekleme.getMaxGun());
        b.setCokUzunGun(bekleme.getCokUzunGun());   b.setCokUzunPuan(bekleme.getCokUzunPuan());
        b.setUzunGun(bekleme.getUzunGun());         b.setUzunPuan(bekleme.getUzunPuan());
        b.setOrtaGun(bekleme.getOrtaGun());         b.setOrtaPuan(bekleme.getOrtaPuan());
        b.setKisaGun(bekleme.getKisaGun());         b.setKisaPuan(bekleme.getKisaPuan());
        b.setVarsayilanPuan(bekleme.getVarsayilanPuan());
        c.setBekleme(b);
        return c;
    }

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
