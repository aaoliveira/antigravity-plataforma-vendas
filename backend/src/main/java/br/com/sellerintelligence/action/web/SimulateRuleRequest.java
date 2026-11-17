package br.com.sellerintelligence.action.web;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SimulateRuleRequest(
        @NotNull(message = "Valor observado é obrigatório para simulação")
        BigDecimal observedValue,

        String targetContext,
        boolean persistAlert
) {}
