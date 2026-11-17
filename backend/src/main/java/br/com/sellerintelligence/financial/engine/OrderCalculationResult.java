package br.com.sellerintelligence.financial.engine;

import br.com.sellerintelligence.financial.domain.CalculationQuality;
import br.com.sellerintelligence.financial.domain.CalculationStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCalculationResult(
        UUID orderId,
        UUID ruleSetId,
        Integer ruleVersion,
        CalculationStatus status,
        CalculationQuality quality,
        BigDecimal grossRevenue,
        BigDecimal netRevenue,
        BigDecimal totalCogs,
        BigDecimal totalMarketplaceFees,
        BigDecimal totalShippingSeller,
        BigDecimal totalTaxes,
        BigDecimal totalPackaging,
        BigDecimal totalOtherCosts,
        BigDecimal contributionMarginValue,
        BigDecimal contributionMarginPercentage,
        List<String> missingFields,
        String assumptions,
        List<ItemCalculationResult> items
) {}
