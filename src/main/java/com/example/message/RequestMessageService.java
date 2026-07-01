package com.example.message;

import com.example.enums.Role;
import com.example.notification.NotificationService;
import com.example.request.RequestService;
import com.example.user.User;
import com.example.user.UserService;
import com.example.workflow.Workflow;
import com.example.workflow.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RequestMessageService {

    private final RequestMessageRepository requestMessageRepository;
    private final RequestService requestService;
    private final UserService userService;
    private final NotificationService notificationService;
    /** Ekip mesajlarında atanmış geliştiriciyi bulmak için (servis değil repo -> döngü olmaz). */
    private final WorkflowRepository workflowRepository;

    /** Müşteri ↔ PO kanalı (dış). */
    public List<RequestMessage> getMessages(Long requestId) {
        return requestMessageRepository.findByRequestId(requestId, false);
    }

    /** Ekip içi kanal (PO / SM / Geliştirici). */
    public List<RequestMessage> getInternalMessages(Long requestId) {
        return requestMessageRepository.findByRequestId(requestId, true);
    }

    public void sendMessage(Long requestId, Long senderId, String body) {
        RequestMessage message = kaydet(requestId, senderId, body, false);
        bildir(message.getRequestId(), senderId);
    }

    /** Ekip içi (dahili) yorum: müşteri görmez; PO/SM/atanmış geliştiriciye bildirim gider. */
    public void sendInternalMessage(Long requestId, Long senderId, String body) {
        kaydet(requestId, senderId, body, true);
        bildirDahili(requestId, senderId);
    }

    private RequestMessage kaydet(Long requestId, Long senderId, String body, boolean internal) {
        if (requestId == null || senderId == null) {
            throw new IllegalArgumentException("Talep ve gönderen zorunludur.");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Mesaj boş olamaz.");
        }
        if (body.length() > 2000) {
            throw new IllegalArgumentException("Mesaj en fazla 2000 karakter olabilir.");
        }
        RequestMessage message = new RequestMessage();
        message.setRequestId(requestId);
        message.setSenderId(senderId);
        message.setBody(body.trim());
        message.setInternal(internal);
        requestMessageRepository.save(message);
        return message;
    }

    /** Mesajı kim yazdıysa karşı tarafa bildirim: müşteri yazdıysa PO'lara, PO yazdıysa müşteriye. */
    private void bildir(Long requestId, Long senderId) {
        requestService.findById(requestId).ifPresent(r -> {
            String baslik = r.getTitle();
            if (senderId.equals(r.getCustomerId())) {
                // Müşteri yazdı -> ürün sorumlularına
                for (User po : userService.findActiveByRole(Role.PRODUCT_OWNER)) {
                    notificationService.notify(po.getUserId(),
                        "Talebe yeni mesaj: " + baslik, requestId);
                }
            } else {
                // PO/yetkili yazdı -> müşteriye
                notificationService.notify(r.getCustomerId(),
                    "Talebinize yeni yanıt: " + baslik, requestId);
            }
        });
    }

    /** Ekip kanalı: tüm aktif PO'lar + SM'ler + talebe atanmış geliştirici (gönderen hariç). */
    private void bildirDahili(Long requestId, Long senderId) {
        requestService.findById(requestId).ifPresent(r -> {
            Set<Long> alicilar = new HashSet<>();
            for (User po : userService.findActiveByRole(Role.PRODUCT_OWNER)) alicilar.add(po.getUserId());
            for (User sm : userService.findActiveByRole(Role.SCRUM_MASTER)) alicilar.add(sm.getUserId());
            workflowRepository.findByRequestId(requestId)
                .map(Workflow::getDeveloperId)
                .ifPresent(devId -> { if (devId != null) alicilar.add(devId); });
            alicilar.remove(senderId);
            for (Long uid : alicilar) {
                notificationService.notify(uid, "Talebe yeni ekip mesajı: " + r.getTitle(), requestId);
            }
        });
    }
}
