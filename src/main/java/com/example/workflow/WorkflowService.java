package com.example.workflow;

import com.example.enums.Role;
import com.example.enums.WorkflowStatus;
import com.example.message.RequestMessage;
import com.example.message.RequestMessageRepository;
import com.example.notification.NotificationService;
import com.example.request.RequestService;
import com.example.request.StatusTransitionValidator;
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
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final StatusTransitionValidator transitionValidator;
    private final RequestService requestService;
    private final NotificationService notificationService;
    private final UserService userService;
    /** Geri gönderme gerekçesini ekip kanalına dahili mesaj olarak işlemek için (repo -> döngü olmaz). */
    private final RequestMessageRepository requestMessageRepository;

    /** requestId -> iş akışı önbelleği. Grid'lerde satır başına çağrıldığı için.
     *  Yoklukları da (Optional.empty) saklar; her yazma işlemi temizler. */
    private final Map<Long, Optional<Workflow>> byRequestCache = new ConcurrentHashMap<>();

    public List<Workflow> getDeveloperWorkflows(Long developerId) {
        return workflowRepository.findByDeveloperId(developerId);
    }

    public List<Workflow> getUnassignedWorkflows() {
        return workflowRepository.findUnassigned();
    }

    public Optional<Workflow> findByRequestId(Long requestId) {
        if (requestId == null) return Optional.empty();
        return byRequestCache.computeIfAbsent(requestId, workflowRepository::findByRequestId);
    }

    public void createWorkflow(Long requestId) {
        if (workflowRepository.findByRequestId(requestId).isPresent()) {
            throw new IllegalStateException("Bu talep zaten iş akışında.");
        }

        Workflow workflow = new Workflow();
        workflow.setRequestId(requestId);
        workflow.setWorkflowStatus(WorkflowStatus.BACKLOG);
        workflow.setVersion(0);

        workflowRepository.save(workflow);
        byRequestCache.clear();

        requestService.findById(requestId).ifPresent(r ->
            notificationService.notify(r.getCustomerId(),
                "Talebiniz iş akışına alındı: " + r.getTitle(), requestId));
    }

    public List<Workflow> getDoneWorkflowsByDeveloper(Long developerId) {
        return workflowRepository.findDoneByDeveloperId(developerId);
    }

    public void updateStatus(Long taskId, WorkflowStatus newStatus, int currentVersion) {
        Workflow workflow = workflowRepository.findByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Görev bulunamadı."));

        if (!transitionValidator.isValidWorkflowTransition(workflow.getWorkflowStatus(), newStatus)) {
            throw new IllegalStateException(
                "Geçersiz durum geçişi: " + workflow.getWorkflowStatus() + " → " + newStatus
            );
        }

        int updated = workflowRepository.updateStatus(taskId, newStatus, currentVersion);
        if (updated == 0) {
            throw new IllegalStateException("Görev başkası tarafından güncellendi. Sayfayı yenileyin.");
        }
        byRequestCache.clear();

        if (newStatus == WorkflowStatus.DONE) {
            requestService.findById(workflow.getRequestId()).ifPresent(r ->
                notificationService.notify(r.getCustomerId(),
                    "Talebiniz tamamlandı: " + r.getTitle(), workflow.getRequestId()));
        }
    }

    public List<Workflow> getAllActiveWorkflows() {
        return workflowRepository.findAllActive();
    }

    public List<Workflow> getAllWorkflows() {
        return workflowRepository.findAll();
    }

    public void assignDeveloperBySM(Long taskId, Long developerId, int currentVersion) {
        int updated = workflowRepository.assignDeveloperBySM(taskId, developerId, currentVersion);
        if (updated == 0) {
            throw new IllegalStateException("Görev zaten atanmış veya başkası güncelledi.");
        }
        byRequestCache.clear();

        workflowRepository.findByTaskId(taskId).ifPresent(w ->
            requestService.findById(w.getRequestId()).ifPresent(r ->
                notificationService.notify(developerId,
                    "Size yeni görev atandı: " + r.getTitle(), w.getRequestId())));
    }

    /**
     * Geliştirici, başladığı bir görevi (IN_PROGRESS/TESTING) gerekçeyle Scrum Master'a geri gönderir.
     * Görev BACKLOG'a döner, gerekçe ekip kanalına işlenir ve tüm Scrum Master'lara bildirim gider.
     */
    public void sendBackToScrumMaster(Long taskId, int version, Long byUserId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Geri gönderme gerekçesi zorunludur.");
        }
        Workflow w = workflowRepository.findByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Görev bulunamadı."));
        if (w.getWorkflowStatus() != WorkflowStatus.IN_PROGRESS
                && w.getWorkflowStatus() != WorkflowStatus.TESTING) {
            throw new IllegalStateException("Yalnızca başlanmış görev Scrum Master'a geri gönderilebilir.");
        }

        int updated = workflowRepository.updateStatus(taskId, WorkflowStatus.BACKLOG, version);
        if (updated == 0) {
            throw new IllegalStateException("Görev başkası tarafından güncellendi. Sayfayı yenileyin.");
        }
        byRequestCache.clear();

        kaydetDahiliMesaj(w.getRequestId(), byUserId,
            "🔙 Scrum Master'a geri gönderildi. Gerekçe: " + reason.trim());

        requestService.findById(w.getRequestId()).ifPresent(r -> {
            for (User sm : userService.findActiveByRole(Role.SCRUM_MASTER)) {
                notificationService.notify(sm.getUserId(),
                    "Görev geri gönderildi: " + r.getTitle(), w.getRequestId());
            }
        });
    }

    /**
     * Scrum Master, henüz başlanmamış (BACKLOG) bir görevi gerekçeyle Ürün Sorumlusuna geri gönderir.
     * İş akışı silinir, talep tekrar "İncelemede"ye döner, gerekçe ekip kanalına işlenir
     * ve tüm Ürün Sorumlularına bildirim gider.
     */
    public void sendBackToProductOwner(Long taskId, Long byUserId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Geri gönderme gerekçesi zorunludur.");
        }
        Workflow w = workflowRepository.findByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Görev bulunamadı."));
        if (w.getWorkflowStatus() != WorkflowStatus.BACKLOG) {
            throw new IllegalStateException(
                "Yalnızca sırada bekleyen (başlanmamış) görev Ürün Sorumlusuna geri gönderilebilir.");
        }

        Long requestId = w.getRequestId();
        workflowRepository.deleteByTaskId(taskId);
        byRequestCache.clear();

        requestService.revertToUnderReview(requestId);

        kaydetDahiliMesaj(requestId, byUserId,
            "🔙 Ürün Sorumlusuna geri gönderildi (iş akışından çıkarıldı). Gerekçe: " + reason.trim());

        requestService.findById(requestId).ifPresent(r -> {
            for (User po : userService.findActiveByRole(Role.PRODUCT_OWNER)) {
                notificationService.notify(po.getUserId(),
                    "Talep yeniden değerlendirme için geri gönderildi: " + r.getTitle(), requestId);
            }
        });
    }

    private void kaydetDahiliMesaj(Long requestId, Long senderId, String body) {
        RequestMessage m = new RequestMessage();
        m.setRequestId(requestId);
        m.setSenderId(senderId);
        m.setBody(body);
        m.setInternal(true);
        requestMessageRepository.save(m);
    }
}
