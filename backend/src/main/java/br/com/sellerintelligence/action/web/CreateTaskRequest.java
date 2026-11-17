package br.com.sellerintelligence.action.web;

import br.com.sellerintelligence.action.domain.TaskPriority;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public record CreateTaskRequest(
        @NotBlank(message = "Título da tarefa é obrigatório")
        String title,

        @NotBlank(message = "Descrição da tarefa é obrigatória")
        String description,

        TaskPriority priority,
        UUID assignedToUserId,
        Instant dueDate,
        String evidenceSummary
) {}
