package com.example.notification;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserNotification {
    private Long notificationId;
    private Long userId;
    private String message;
    private Long requestId;
    private boolean read;
    private LocalDateTime createdAt;
}
