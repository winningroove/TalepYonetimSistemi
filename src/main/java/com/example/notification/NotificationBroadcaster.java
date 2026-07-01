package com.example.notification;

import com.vaadin.flow.shared.Registration;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Kullanıcı bazlı canlı bildirim yayını. Her açık ekrandaki bildirim zili
 * kendi kullanıcısı için bir dinleyici kaydeder; bir olay olunca o kullanıcının
 * dinleyicileri tetiklenir (dinleyici, UI.access ile tarayıcıya canlı yansıtır).
 */
@Component
public class NotificationBroadcaster {

    private final Map<Long, List<Runnable>> listeners = new ConcurrentHashMap<>();

    public Registration register(Long userId, Runnable onEvent) {
        listeners.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(onEvent);
        return () -> {
            List<Runnable> ls = listeners.get(userId);
            if (ls != null) {
                ls.remove(onEvent);
                if (ls.isEmpty()) listeners.remove(userId);
            }
        };
    }

    public void broadcast(Long userId) {
        if (userId == null) return;
        List<Runnable> ls = listeners.get(userId);
        if (ls != null) {
            ls.forEach(Runnable::run);
        }
    }
}
