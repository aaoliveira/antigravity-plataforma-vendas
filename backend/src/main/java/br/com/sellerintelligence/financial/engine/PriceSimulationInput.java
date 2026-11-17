package br.com.sellerintelligence.financial.engine;

import java.math.BigDecimal;

public record PriceSimulationInput(
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
        BigDecimal targetMarginAmount
) {}
