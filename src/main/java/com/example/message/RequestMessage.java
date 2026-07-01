package com.example.message;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RequestMessage {
    private Long messageId;
    private Long requestId;
    private Long senderId;
    private String body;
    /** true ise ekip içi (dahili) yorum: PO/SM/Geliştirici görür, müşteri görmez. */
    private boolean internal;
    private LocalDateTime createdAt;
}
