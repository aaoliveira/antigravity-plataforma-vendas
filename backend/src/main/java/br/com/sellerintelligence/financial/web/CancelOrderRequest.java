package br.com.sellerintelligence.financial.web;

import jakarta.validation.constraints.NotBlank;

public record CancelOrderRequest(
        @NotBlank(message = "Motivo do cancelamento é obrigatório")
        String reason
) {}
