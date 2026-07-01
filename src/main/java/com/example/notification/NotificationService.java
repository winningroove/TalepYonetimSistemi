package com.example.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationBroadcaster broadcaster;

    /** Bildirim oluşturur (DB) ve ilgili kullanıcının açık ekranlarına canlı yayar. */
    public void notify(Long userId, String message, Long requestId) {
        if (userId == null || message == null || message.isBlank()) return;
        UserNotification n = new UserNotification();
        n.setUserId(userId);
        n.setMessage(message);
        n.setRequestId(requestId);
        notificationRepository.save(n);
        broadcaster.broadcast(userId);
    }

    public int getUnreadCount(Long userId) {
        return notificationRepository.countUnread(userId);
    }

    public List<UserNotification> getRecent(Long userId) {
        return notificationRepository.findRecent(userId);
    }

    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId);
    }

    /** Kullanıcının tüm bildirimlerini siler ve rozeti güncellemek için canlı yayar. */
    public void clearAll(Long userId) {
        notificationRepository.deleteAll(userId);
        broadcaster.broadcast(userId);
    }
}
