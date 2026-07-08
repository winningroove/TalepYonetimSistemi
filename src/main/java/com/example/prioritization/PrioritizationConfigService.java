package com.example.prioritization;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PrioritizationConfigService {

    public static final List<String> AGIRLIK_KEYS = List.of(
        "agirlik.isEtkisi", "agirlik.aciliyet", "agirlik.musteriDegeri",
        "agirlik.isTipi", "agirlik.beklemeSuresi", "agirlik.bolen");
    public static final List<String> ETIKET_KEYS = List.of(
        "etiket.cokDusuk", "etiket.dusuk", "etiket.orta", "etiket.yuksek");
    public static final List<String> GUVENILIRLIK_KEYS = List.of(
        "guvenilirlik.minTalep", "guvenilirlik.redOraniEsik",
        "guvenilirlik.onayOraniEsik", "guvenilirlik.ceza", "guvenilirlik.odul");
    public static final List<String> BEKLEME_KEYS = List.of(
        "bekleme.cokUzunPuan", "bekleme.uzunPuan", "bekleme.ortaPuan",
        "bekleme.kisaPuan", "bekleme.varsayilanPuan");

    private final PrioritizationProperties defaults;
    private final PrioritizationConfigRepository repository;

    private volatile PrioritizationProperties effective;

    public PrioritizationConfigService(PrioritizationProperties defaults,
                                       PrioritizationConfigRepository repository) {
        this.defaults = defaults;
        this.repository = repository;
    }

    @PostConstruct
    void init() {
        reload();
    }

    /** Skor hesaplamasında ve dialoglarda kullanılan etkin ayarlar. */
    public PrioritizationProperties current() {
        return effective;
    }

    /** Bir ayarın etkin (güncel) sayısal değeri — form alanlarını doldurmak için. */
    public double get(String key) {
        Object v = new BeanWrapperImpl(effective).getPropertyValue(key);
        return v == null ? 0 : ((Number) v).doubleValue();
    }

    /** Verilen ayarları DB'ye yazar ve etkin değerleri yeniler. */
    public synchronized void save(Map<String, ? extends Number> values) {
        values.forEach((k, v) -> repository.upsert(k, v.doubleValue()));
        reload();
    }

    /** Tüm override'ları siler; varsayılanlara döner. */
    public synchronized void resetToDefaults() {
        repository.deleteAll();
        reload();
    }

    /** Varsayılanları kopyalar, üstüne DB override'larını uygular, cache'e alır. */
    public synchronized void reload() {
        PrioritizationProperties p = defaults.copy();
        Map<String, Double> overrides;
        try {
            overrides = repository.findAll();
        } catch (Exception e) {
            // Tablo yoksa / erişilemezse yalnızca varsayılanlarla devam et.
            overrides = Map.of();
        }
        BeanWrapper bw = new BeanWrapperImpl(p);
        overrides.forEach((key, value) -> {
            try {
                bw.setPropertyValue(key, value); // Double -> int/double dönüşümü otomatik
            } catch (Exception ignore) {
                // Bilinmeyen/eski anahtarları yok say.
            }
        });
        effective = p;
    }
}
