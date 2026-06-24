// service/PrioritizationService.java
package com.example.service;

import com.example.model.Prioritization;
import com.example.repository.PrioritizationRepository;
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

    public int calculateScore(Prioritization p) {
        double bazSkor   = calculateBazSkor(p);
        double carpan    = p.getYoneticiMudahalesi().getCarpan();
        int    duzeltici = p.getGelistiriciMudahalesi().getDuzeltici();
        return Math.min(100, Math.max(0, (int) Math.round(bazSkor * carpan + duzeltici)));
    }

    public int calculateBeklemeSuresiPuan(LocalDateTime createdAt) {
        long gun = ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());
        if (gun >= 30) return 5;
        if (gun >= 14) return 4;
        if (gun >= 7)  return 3;
        if (gun >= 3)  return 2;
        return 1;
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

    public void savePrioritization(Prioritization p, Long customerId, LocalDateTime requestCreatedAt) {

        // BR40: musteriDegeriPuan users tablosundan otomatik
        p.setMusteriDegeriPuan(userService.getMusteriDegeriPuan(customerId));

        // BR36: bekleme süresi hesaplanır
        p.setBeklemeSuresiPuan(calculateBeklemeSuresiPuan(requestCreatedAt));

        // isTipi puanı enum'dan alınır
        p.setIsTimiPuan(p.getIsTipi().getPuan());

        // Skorlar hesaplanır
        p.setBazSkor(calculateBazSkor(p));
        p.setPriorityScore(calculateScore(p));

        // Kaydet veya güncelle
        if (prioritizationRepository.findByRequestId(p.getRequestId()).isPresent()) {
            prioritizationRepository.update(p);
        } else {
            prioritizationRepository.save(p);
        }

        // BR15: talebi PRIORITIZED yap
        requestService.markAsPrioritized(p.getRequestId());
    }
}