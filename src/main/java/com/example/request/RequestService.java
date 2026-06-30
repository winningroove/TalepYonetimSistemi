package com.example.request;

import com.example.enums.RequestStatus;
import com.example.enums.YoneticiTakdiri;
import com.example.request.RequestRepository.CredibilityStats;
import com.example.user.User;
import com.example.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final RequestRepository requestRepository;
    private final StatusTransitionValidator transitionValidator;
    private final UserService userService;

    /** requestId -> talep önbelleği (SM/Geliştirici grid'lerinde talep adı/tarih çözümü için).
     *  Yazma metotları kendi doğrulamalarında repository'yi doğrudan kullanır; bu önbellek
     *  yalnızca görüntüleme okumalarını hızlandırır ve her yazmada temizlenir. */
    private final Map<Long, Optional<Request>> idCache = new ConcurrentHashMap<>();

    public void createRequest(Request request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Talep başlığı boş bırakılamaz.");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("Talep detayı boş bırakılamaz.");
        }

        request.setStatus(RequestStatus.NEW);

        requestRepository.save(request);
        idCache.clear();
    }

    public List<Request> getCustomerRequests(Long customerId) {
        return requestRepository.findByCustomerId(customerId);
    }

    public Optional<Request> findById(Long requestId) {
        if (requestId == null) return Optional.empty();
        return idCache.computeIfAbsent(requestId, requestRepository::findById);
    }

    public List<Request> getAllActiveRequests() {
        return requestRepository.findAllActive();
    }

    public List<Request> getByStatus(RequestStatus status) {
        return requestRepository.findByStatus(status);
    }

    public void takeUnderReview(Long requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Talep bulunamadı."));

        if (!transitionValidator.isValidRequestTransition(request.getStatus(), RequestStatus.UNDER_REVIEW)) {
            throw new IllegalStateException("Bu talep incelemeye alınamaz. Mevcut durum: " + request.getStatus());
        }

        requestRepository.updateStatus(requestId, RequestStatus.UNDER_REVIEW);
        idCache.clear();
    }

    public void rejectRequest(Long requestId, String rejectionReason) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Talep bulunamadı."));

        if (!transitionValidator.isValidRequestTransition(request.getStatus(), RequestStatus.REJECTED)) {
            throw new IllegalStateException("Bu talep reddedilemez. Mevcut durum: " + request.getStatus());
        }

        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new IllegalArgumentException("Ret gerekçesi zorunludur.");
        }

        requestRepository.reject(requestId, rejectionReason);
        idCache.clear();
    }

    public Optional<Long> findLastRequestIdByCustomer(Long customerId) {
        return requestRepository.findLastRequestIdByCustomer(customerId);
    }

    public void markAsPrioritized(Long requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Talep bulunamadı."));

        if (!transitionValidator.isValidRequestTransition(request.getStatus(), RequestStatus.PRIORITIZED)) {
            throw new IllegalStateException("Bu talep önceliklendirilemez. Mevcut durum: " + request.getStatus());
        }

        requestRepository.updateStatus(requestId, RequestStatus.PRIORITIZED);
        idCache.clear();
    }

    public void updateYoneticiTakdiri(Long requestId, YoneticiTakdiri takdir) {
        requestRepository.updateYoneticiTakdiri(requestId, takdir);
        idCache.clear();
    }

    public CredibilityStats getCredibilityStats(Long customerId) {
        return requestRepository.getCredibilityStats(customerId);
    }

    private String normalizeTitle(String title) {
        return title == null ? "" : title.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    /**
     * Verilen talebin olası kopyaları: aynı şirketten, aktif (NEW/UNDER_REVIEW/PRIORITIZED),
     * henüz birleştirilmemiş ve normalize edilmiş başlığı birebir eşleşen diğer talepler.
     * Müşterinin şirketi yoksa boş liste döner.
     */
    public List<Request> findPotentialDuplicates(Request request) {
        User customer = userService.findById(request.getCustomerId()).orElse(null);
        if (customer == null || customer.getCompanyId() == null) {
            return List.of();
        }
        String norm = normalizeTitle(request.getTitle());
        return requestRepository.findActiveByCompany(customer.getCompanyId()).stream()
                .filter(r -> !r.getRequestId().equals(request.getRequestId()))
                .filter(r -> normalizeTitle(r.getTitle()).equals(norm))
                .toList();
    }

    /**
     * Kopya talebi ana talebe bağlar (DUPLICATE durumuna geçirir).
     * Yalnızca aktif ve iş akışına girmemiş talepler birleştirilebilir.
     */
    public void mergeDuplicate(Long canonicalId, Long duplicateId) {
        if (canonicalId.equals(duplicateId)) {
            throw new IllegalArgumentException("Bir talep kendisiyle birleştirilemez.");
        }
        Request canonical = requestRepository.findById(canonicalId)
                .orElseThrow(() -> new IllegalArgumentException("Ana talep bulunamadı."));
        Request duplicate = requestRepository.findById(duplicateId)
                .orElseThrow(() -> new IllegalArgumentException("Kopya talep bulunamadı."));

        if (!isActive(canonical.getStatus())) {
            throw new IllegalStateException("Ana talep aktif değil, birleştirme yapılamaz.");
        }
        if (!transitionValidator.isValidRequestTransition(duplicate.getStatus(), RequestStatus.DUPLICATE)) {
            throw new IllegalStateException("Bu talep birleştirilemez. Mevcut durum: " + duplicate.getStatus());
        }

        requestRepository.markAsDuplicate(duplicateId, canonicalId);
        idCache.clear();
    }

    private boolean isActive(RequestStatus status) {
        return status == RequestStatus.NEW
            || status == RequestStatus.UNDER_REVIEW
            || status == RequestStatus.PRIORITIZED;
    }
}
