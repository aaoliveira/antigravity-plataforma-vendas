package br.com.sellerintelligence.action.web;

import br.com.sellerintelligence.action.domain.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(
        @NotNull(message = "Status é obrigatório")
        TaskStatus status,

        String notes
) {}
