package br.com.sellerintelligence.financial.engine;

import br.com.sellerintelligence.financial.domain.CalculationStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCalculationInput(
        UUID orderId,
        UUID ruleSetId,
        Integer ruleVersion,
        CalculationStatus status,
        List<ItemCalculationInput> items,
        BigDecimal orderLevelShippingSeller,
        BigDecimal orderLevelPackagingCost,
        BigDecimal orderLevelOtherCosts
) {}
