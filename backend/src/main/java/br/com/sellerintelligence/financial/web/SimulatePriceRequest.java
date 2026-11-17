package br.com.sellerintelligence.financial.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record SimulatePriceRequest(
        @NotNull(message = "Custo unitário (COGS) é obrigatório para simulação")
        @PositiveOrZero(message = "Custo deve ser positivo ou zero")
        BigDecimal unitCogs,

        BigDecimal packagingCost,
        BigDecimal shippingSeller,
        BigDecimal fixedFee,
        BigDecimal fixedFeeThreshold,
        BigDecimal commissionPercentage,
        BigDecimal taxPercentage,
        BigDecimal otherVariablePercentage,
        BigDecimal otherVariableFixed,
        BigDecimal targetMarginPercentage,
        BigDecimal targetMarginAmount,
        BigDecimal trialPrice
) {}
