package com.example.activity;

import lombok.Data;
import java.time.LocalDateTime;

/** Bir talep üzerindeki tek bir işlem/durum değişikliği kaydı (audit trail). */
@Data
public class ActivityLog {
    private Long activityId;
    private Long requestId;
    private Long userId;      // işlemi yapan kullanıcı (null = sistem)
    private String action;    // kısa etiket, örn. "Önceliklendirildi"
    private String detail;    // opsiyonel ayrıntı
    private LocalDateTime createdAt;
}
