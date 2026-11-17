package br.com.sellerintelligence.reconciliation.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssociateOrderRequest(
        @NotNull(message = "orderId é obrigatório")
        UUID orderId,
        @NotBlank(message = "justification é obrigatória")
        String justification
) {}
