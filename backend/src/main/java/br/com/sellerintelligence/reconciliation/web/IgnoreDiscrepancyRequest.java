package br.com.sellerintelligence.reconciliation.web;

import jakarta.validation.constraints.NotBlank;

public record IgnoreDiscrepancyRequest(
        @NotBlank(message = "justification é obrigatória")
        String justification
) {}
