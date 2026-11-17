package br.com.sellerintelligence.financial.dashboard;

import br.com.sellerintelligence.financial.domain.CalculationQuality;

import java.math.BigDecimal;

public record ProductProfitabilityDto(
        String sku,
        String title,
        long quantitySold,
        BigDecimal grossRevenue,
        BigDecimal totalCogs,
        BigDecimal contributionMarginValue,
        BigDecimal contributionMarginPercentage,
        CalculationQuality quality
) {}
