package com.example.prioritization;

import com.example.enums.GelistiriciMudahalesi;
import com.example.enums.YoneticiTakdiri;
import com.example.request.Request;
import com.example.request.RequestRepository.CredibilityStats;
import com.example.request.RequestService;
import com.example.user.UserService;
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

    private double calculateBazSkor(Prioritization p) {
        return (
            p.getIsEtkisi()          * 30 +
            p.getAciliyet()          * 25 +
            p.getMusteriDegeriPuan() * 20 +
            p.getIsTimiPuan()        * 15 +
            p.getBeklemeSuresiPuan() * 10
        ) / 5.0;
    }

    public int calculateBeklemeSuresiPuan(LocalDateTime createdAt) {
        long gun = Math.min(30, ChronoUnit.DAYS.between(createdAt, LocalDateTime.now()));
        if (gun >= 30) return 5;
        if (gun >= 14) return 4;
        if (gun >= 7)  return 3;
        if (gun >= 3)  return 2;
        return 1;
    }

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

    public int calculateFinalScore(Prioritization p, YoneticiTakdiri takdir, int credibilityScore) {
        double bazSkor       = calculateBazSkor(p);
        int    takdirPuan    = (takdir != null ? takdir.getPuan() : 0);
        int    cabaDuzeltici = (p.getGelistiriciMudahalesi() != null
                ? p.getGelistiriciMudahalesi().getDuzeltici()
                : 0);

        int skor = (int) Math.round(bazSkor + takdirPuan + credibilityScore + cabaDuzeltici);
        return Math.min(100, Math.max(0, skor));
    }

    // Baz skor matematiksel olarak 20-100 aralığına sıkışır (her faktör 1-5,
    // alt sınır asla 0 değil), bu yüzden bantlar 0-100 değil gerçek 20-100
    // aralığına göre kalibre edilmiştir.
    public String getLabel(int score) {
        if (score == 0)  return "İPTAL";
        if (score <= 36) return "ÇOK DÜŞÜK";
        if (score <= 52) return "DÜŞÜK";
        if (score <= 68) return "ORTA";
        if (score <= 84) return "YÜKSEK";
        return "KRİTİK";
    }

    public Optional<Prioritization> findByRequestId(Long requestId) {
        return prioritizationRepository.findByRequestId(requestId);
    }

    public void savePrioritizationByPO(Prioritization p, Long customerId, LocalDateTime requestCreatedAt) {
        p.setMusteriDegeriPuan(userService.getMusteriDegeriPuan(customerId));
        p.setBeklemeSuresiPuan(calculateBeklemeSuresiPuan(requestCreatedAt));
        p.setIsTimiPuan(p.getIsTipi().getPuan());

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
