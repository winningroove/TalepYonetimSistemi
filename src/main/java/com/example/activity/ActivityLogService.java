package com.example.activity;

import com.example.user.User;
import com.example.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository repository;
    private final UserService userService;

    public List<ActivityLog> getByRequestId(Long requestId) {
        if (requestId == null) return List.of();
        return repository.findByRequestId(requestId);
    }

    /** Bir kullanıcının son yaptığı işlemler (profil ekranı için). */
    public List<ActivityLog> getRecentByUser(Long userId) {
        if (userId == null) return List.of();
        return repository.findRecentByUserId(userId);
    }

    /** Eylemi, o an giriş yapmış kullanıcı adına kaydeder. */
    public void log(Long requestId, String action, String detail) {
        if (requestId == null || action == null) return;
        ActivityLog a = new ActivityLog();
        a.setRequestId(requestId);
        a.setUserId(currentUserId());
        a.setAction(action);
        a.setDetail(detail);
        repository.save(a);
    }

    private Long currentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) return null;
            String email = auth.getName();
            if (email == null || "anonymousUser".equals(email)) return null;
            return userService.findByEmail(email).map(User::getUserId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
