package br.com.sellerintelligence.financial.web;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record RecalculateOrderRequest(
        UUID ruleSetId,

        @NotBlank(message = "Motivo do recálculo é obrigatório para auditoria")
        String auditReason
) {}
