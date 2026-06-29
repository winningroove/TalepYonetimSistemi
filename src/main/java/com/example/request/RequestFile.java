package com.example.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RequestFile {
    private Long fileId;
    private Long requestId;
    private String fileName;
    private byte[] fileData;
    private Long fileSize;
    private LocalDateTime createdAt;
}
