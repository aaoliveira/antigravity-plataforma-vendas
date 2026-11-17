package br.com.sellerintelligence.action.web;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignTaskRequest(
        @NotNull(message = "assignedToUserId é obrigatório")
        UUID assignedToUserId
) {}
