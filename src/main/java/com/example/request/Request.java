package com.example.request;

import com.example.enums.RequestStatus;
import com.example.enums.YoneticiTakdiri;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Request {
    private Long requestId;
    private Long customerId;
    private String title;
    private String description;
    private RequestStatus status;
    private String rejectionReason;
    private YoneticiTakdiri yoneticiTakdiri;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
