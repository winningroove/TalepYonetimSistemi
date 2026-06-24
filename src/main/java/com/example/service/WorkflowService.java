// service/WorkflowService.java
package com.example.service;

import com.example.enums.WorkflowStatus;
import com.example.model.Workflow;
import com.example.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final StatusTransitionValidator transitionValidator;

    public List<Workflow> getDeveloperWorkflows(Long developerId) {
        return workflowRepository.findByDeveloperId(developerId);
    }

    public List<Workflow> getUnassignedWorkflows() {
        return workflowRepository.findUnassigned();
    }

    public Optional<Workflow> findByRequestId(Long requestId) {
        return workflowRepository.findByRequestId(requestId);
    }

    public void createWorkflow(Long requestId) {
        if (workflowRepository.findByRequestId(requestId).isPresent()) {
            throw new IllegalStateException("Bu talep zaten iş akışında.");
        }

        Workflow workflow = new Workflow();
        workflow.setRequestId(requestId);
        workflow.setWorkflowStatus(WorkflowStatus.BACKLOG);
        workflow.setVersion(0);

        workflowRepository.save(workflow);
    }

    public void assignDeveloper(Long taskId, Long developerId, int currentVersion) {
        int updated = workflowRepository.assignDeveloperBySM(taskId, developerId, currentVersion);
        if (updated == 0) {
            throw new IllegalStateException("Bu görev başkası tarafından üstlenildi. Sayfayı yenileyin.");
        }
    }
public List<Workflow> getDoneWorkflowsByDeveloper(Long developerId) {
    return workflowRepository.findDoneByDeveloperId(developerId);
}
public void updateStatus(Long taskId, WorkflowStatus newStatus, int currentVersion) {
    Workflow workflow = workflowRepository.findByTaskId(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Görev bulunamadı."));

    if (!transitionValidator.isValidWorkflowTransition(workflow.getWorkflowStatus(), newStatus)) {
        throw new IllegalStateException(
            "Geçersiz durum geçişi: " + workflow.getWorkflowStatus() + " → " + newStatus
        );
    }

    int updated = workflowRepository.updateStatus(taskId, newStatus, currentVersion);
    if (updated == 0) {
        throw new IllegalStateException("Görev başkası tarafından güncellendi. Sayfayı yenileyin.");
    }
}
public List<Workflow> getAllActiveWorkflows() {
    return workflowRepository.findAllActive();
}

public List<Workflow> getAllWorkflows() {
    return workflowRepository.findAll();
}

public void assignDeveloperBySM(Long taskId, Long developerId, int currentVersion) {
    int updated = workflowRepository.assignDeveloperBySM(taskId, developerId, currentVersion);
    if (updated == 0) {
        throw new IllegalStateException("Görev zaten atanmış veya başkası güncelledi.");
    }
}
}