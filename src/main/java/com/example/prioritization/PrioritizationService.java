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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PrioritizationService {

    private final PrioritizationRepository prioritizationRepository;
    private final RequestService requestService;
    private final UserService userService;
    private final PrioritizationProperties props;

    private final Map<Long, Optional<Prioritization>> byRequestCache = new ConcurrentHashMap<>();

    /** Skor katsayıları ve eşikleri (dialoglardaki dağılım gösterimi de bunu kullanır). */
    public PrioritizationProperties getProperties() {
        return props;
    }

    private double calculateBazSkor(Prioritization p) {
        PrioritizationProperties.Agirlik a = props.getAgirlik();
        return (
            p.getIsEtkisi()          * a.getIsEtkisi() +
            p.getAciliyet()          * a.getAciliyet() +
            p.getMusteriDegeriPuan() * a.getMusteriDegeri() +
            p.getIsTipiPuan()        * a.getIsTipi() +
            p.getBeklemeSuresiPuan() * a.getBeklemeSuresi()
        ) / a.getBolen();
    }

    public int calculateBeklemeSuresiPuan(LocalDateTime createdAt) {
        PrioritizationProperties.Bekleme b = props.getBekleme();
        long gun = Math.min(b.getMaxGun(), ChronoUnit.DAYS.between(createdAt, LocalDateTime.now()));
        if (gun >= b.getCokUzunGun()) return b.getCokUzunPuan();
        if (gun >= b.getUzunGun())    return b.getUzunPuan();
        if (gun >= b.getOrtaGun())    return b.getOrtaPuan();
        if (gun >= b.getKisaGun())    return b.getKisaPuan();
        return b.getVarsayilanPuan();
    }

    public int calculateCredibilityScore(Long customerId) {
        PrioritizationProperties.Guvenilirlik g = props.getGuvenilirlik();
        CredibilityStats s = requestService.getCredibilityStats(customerId);

        if (s.total() < g.getMinTalep()) {
            return 0;
        }

        double reddedilmeOrani = (double) s.rejected() / s.total();
        double onaylanmaOrani  = (double) s.approved() / s.total();

        if (reddedilmeOrani > g.getRedOraniEsik())  return g.getCeza();
        if (onaylanmaOrani  >= g.getOnayOraniEsik()) return g.getOdul();
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

    
    public String getLabel(int score) {
        PrioritizationProperties.Etiket e = props.getEtiket();
        if (score == 0)              return "İPTAL";
        if (score <= e.getCokDusuk()) return "ÇOK DÜŞÜK";
        if (score <= e.getDusuk())    return "DÜŞÜK";
        if (score <= e.getOrta())     return "ORTA";
        if (score <= e.getYuksek())   return "YÜKSEK";
        return "KRİTİK";
    }

    public Optional<Prioritization> findByRequestId(Long requestId) {
        if (requestId == null) return Optional.empty();
        return byRequestCache.computeIfAbsent(requestId, prioritizationRepository::findByRequestId);
    }

    public void savePrioritizationByPO(Prioritization p, Long customerId, LocalDateTime requestCreatedAt) {
        p.setMusteriDegeriPuan(userService.getMusteriDegeriPuan(customerId));
        p.setBeklemeSuresiPuan(calculateBeklemeSuresiPuan(requestCreatedAt));
        p.setIsTipiPuan(p.getIsTipi().getPuan());

        p.setBazSkor(0);
        p.setPriorityScore(0);
        p.setGelistiriciMudahalesi(null);

        if (prioritizationRepository.findByRequestId(p.getRequestId()).isPresent()) {
            prioritizationRepository.update(p);
        } else {
            prioritizationRepository.save(p);
        }

        requestService.markAsPrioritized(p.getRequestId());
        byRequestCache.clear();
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
        byRequestCache.clear();
    }
}
