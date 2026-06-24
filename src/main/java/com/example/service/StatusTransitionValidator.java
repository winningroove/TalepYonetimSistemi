// service/StatusTransitionValidator.java
package com.example.service;

import com.example.enums.RequestStatus;
import com.example.enums.WorkflowStatus;
import org.springframework.stereotype.Component;

@Component
public class StatusTransitionValidator {

    public boolean isValidRequestTransition(RequestStatus current, RequestStatus next) {
        return switch (current) {
            case NEW          -> next == RequestStatus.UNDER_REVIEW;
            case UNDER_REVIEW -> next == RequestStatus.PRIORITIZED || next == RequestStatus.REJECTED;
            default           -> false;
        };
    }

    public boolean isValidWorkflowTransition(WorkflowStatus current, WorkflowStatus next) {
        return switch (current) {
            case BACKLOG     -> next == WorkflowStatus.IN_PROGRESS;
            case IN_PROGRESS -> next == WorkflowStatus.TESTING;
            case TESTING     -> next == WorkflowStatus.DONE;
            default          -> false;
        };
    }
}