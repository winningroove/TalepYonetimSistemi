// model/Workflow.java
package com.example.model;

import com.example.enums.WorkflowStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Workflow {
    private Long taskId;
    private Long requestId;
    private Long developerId;
    private WorkflowStatus workflowStatus;
    private int version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}