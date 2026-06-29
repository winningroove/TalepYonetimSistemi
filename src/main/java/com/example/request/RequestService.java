package com.example.request;

import com.example.enums.RequestStatus;
import com.example.enums.YoneticiTakdiri;
import com.example.request.RequestRepository.CredibilityStats;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final RequestRepository requestRepository;
    private final StatusTransitionValidator transitionValidator;

    public void createRequest(Request request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Talep başlığı boş bırakılamaz.");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("Talep detayı boş bırakılamaz.");
        }

        request.setStatus(RequestStatus.NEW);

        requestRepository.save(request);
    }

    public List<Request> getCustomerRequests(Long customerId) {
        return requestRepository.findByCustomerId(customerId);
    }

    public Optional<Request> findById(Long requestId) {
        return requestRepository.findById(requestId);
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
    }

    public void updateYoneticiTakdiri(Long requestId, YoneticiTakdiri takdir) {
        requestRepository.updateYoneticiTakdiri(requestId, takdir);
    }

    public CredibilityStats getCredibilityStats(Long customerId) {
        return requestRepository.getCredibilityStats(customerId);
    }
}
