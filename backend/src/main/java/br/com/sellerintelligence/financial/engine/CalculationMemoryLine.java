package br.com.sellerintelligence.financial.engine;

import java.math.BigDecimal;

public record CalculationMemoryLine(
        String componentName,
        BigDecimal baseAmount,
        BigDecimal appliedRate,
        BigDecimal resultingAmount,
        String impactType,
        String sourceOrRule,
        String notes
) {}
