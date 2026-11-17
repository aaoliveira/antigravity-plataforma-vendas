package br.com.sellerintelligence.action.web;

import br.com.sellerintelligence.action.domain.TaskPriority;

import java.time.Instant;
import java.util.UUID;

public record CreateTaskFromAlertRequest(
        TaskPriority priority,
        UUID assignedToUserId,
        Instant dueDate,
        String customTitle,
        String customNotes
) {}
