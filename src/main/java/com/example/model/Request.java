// model/Request.java
package com.example.model;

import com.example.enums.RequestStatus;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}