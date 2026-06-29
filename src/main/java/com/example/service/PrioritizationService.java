// service/PrioritizationService.java
package com.example.service;

import com.example.enums.GelistiriciMudahalesi;
import com.example.enums.YoneticiTakdiri;
import com.example.model.Prioritization;
import com.example.model.Request;
import com.example.repository.PrioritizationRepository;
import com.example.repository.RequestRepository.CredibilityStats;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PrioritizationService {

    private final PrioritizationRepository prioritizationRepository;
    private final RequestService requestService;
    private final UserService userService;

    // ── Baz skor (0-100 aralığında ağırlıklı ortalama) ──
    private double calculateBazSkor(Prioritization p) {
        return (
            p.getIsEtkisi()          * 30 +
            p.getAciliyet()          * 25 +
            p.getMusteriDegeriPuan() * 20 +
            p.getIsTimiPuan()        * 15 +
            p.getBeklemeSuresiPuan() * 10
        ) / 5.0;
    }

    /**
     * Bekleme süresi puanı. Maksimum bekleme 30 gün ile sınırlıdır
     * (30 günden eski talepler 30 gün gibi değerlendirilir).
     */
    public int calculateBeklemeSuresiPuan(LocalDateTime createdAt) {
        long gun = Math.min(30, ChronoUnit.DAYS.between(createdAt, LocalDateTime.now()));
        if (gun >= 30) return 5;
        if (gun >= 14) return 4;
        if (gun >= 7)  return 3;
        if (gun >= 3)  return 2;
        return 1;
    }

    /**
     * Requester Credibility (güvenilirlik) skoru — talep sahibinin geçmiş performansı.
     *  - Toplam talep < 5  -> 0 (yeterli geçmiş yok)
     *  - Reddedilme oranı > %70 -> -10 (ceza)
     *  - Onaylanma oranı  > %80 -> +5  (ödül)
     *  - Aksi halde 0
     */
    public int calculateCredibilityScore(Long customerId) {
        CredibilityStats s = requestService.getCredibilityStats(customerId);

        if (s.total() < 5) {
            return 0;
        }

        double reddedilmeOrani = (double) s.rejected() / s.total();
        double onaylanmaOrani  = (double) s.approved() / s.total();

        if (reddedilmeOrani > 0.70) return -10;
        if (onaylanmaOrani  > 0.80) return 5;
        return 0;
    }

    /**
     * Final skor formülü:
     *   Baz Skor + Yönetici Takdiri Puanı + Güvenilirlik Skoru - Geliştirici Çaba Cezası
     *
     * Geliştirici çaba düzelticisi (GelistiriciMudahalesi.getDuzeltici) yüksek çabada
     * negatif, hızlı işte pozitif olduğundan "- çaba cezası" ile matematiksel olarak
     * eşdeğerdir (ceza = -düzeltici); bu yüzden doğrudan toplanır.
     *
     * Sonuç her durumda 0-100 aralığına sıkıştırılır.
     */
    public int calculateFinalScore(Prioritization p, YoneticiTakdiri takdir, int credibilityScore) {
        double bazSkor       = calculateBazSkor(p);
        int    takdirPuan    = (takdir != null ? takdir.getPuan() : 0);
        int    cabaDuzeltici = (p.getGelistiriciMudahalesi() != null
                ? p.getGelistiriciMudahalesi().getDuzeltici()
                : 0);

        int skor = (int) Math.round(bazSkor + takdirPuan + credibilityScore + cabaDuzeltici);
        return Math.min(100, Math.max(0, skor));
    }

    public String getLabel(int score) {
        if (score == 0)  return "İPTAL";
        if (score <= 20) return "ÇOK DÜŞÜK";
        if (score <= 40) return "DÜŞÜK";
        if (score <= 60) return "ORTA";
        if (score <= 80) return "YÜKSEK";
        return "KRİTİK";
    }

    public Optional<Prioritization> findByRequestId(Long requestId) {
        return prioritizationRepository.findByRequestId(requestId);
    }

    /**
     * PO önceliklendirme girdilerini kaydeder. Geliştirici çaba tahmini henüz girilmediği
     * için skor 0 olarak bekletilir; nihai skor Scrum Master çabayı girince hesaplanır.
     * Yönetici Takdiri talep (Eren_requests) üzerinde tutulur ve view tarafından ayrıca yazılır.
     */
    public void savePrioritizationByPO(Prioritization p, Long customerId, LocalDateTime requestCreatedAt) {
        p.setMusteriDegeriPuan(userService.getMusteriDegeriPuan(customerId));
        p.setBeklemeSuresiPuan(calculateBeklemeSuresiPuan(requestCreatedAt));
        p.setIsTimiPuan(p.getIsTipi().getPuan());

        // Skor henüz hesaplanmaz
        p.setBazSkor(0);
        p.setPriorityScore(0);
        p.setGelistiriciMudahalesi(null);

        if (prioritizationRepository.findByRequestId(p.getRequestId()).isPresent()) {
            prioritizationRepository.update(p);
        } else {
            prioritizationRepository.save(p);
        }

        requestService.markAsPrioritized(p.getRequestId());
    }

    /**
     * Scrum Master çaba tahminini ekler ve nihai skoru hesaplar.
     * Yönetici Takdiri ve Güvenilirlik skoru talebe/müşteriye göre bu aşamada okunur.
     */
    public void updateGelistiriciMudahalesi(Long requestId, GelistiriciMudahalesi gelistiriciMudahalesi) {
        Prioritization p = prioritizationRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Önceliklendirme kaydı bulunamadı."));

        Request request = requestService.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Talep bulunamadı."));

        p.setGelistiriciMudahalesi(gelistiriciMudahalesi);
        p.setBeklemeSuresiPuan(calculateBeklemeSuresiPuan(request.getCreatedAt()));
        p.setBazSkor(calculateBazSkor(p));

        int credibility = calculateCredibilityScore(request.getCustomerId());
        p.setPriorityScore(calculateFinalScore(p, request.getYoneticiTakdiri(), credibility));

        prioritizationRepository.update(p);
    }
}
