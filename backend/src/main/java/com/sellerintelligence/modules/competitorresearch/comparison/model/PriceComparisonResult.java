package com.sellerintelligence.modules.competitorresearch.comparison.model;

import java.math.BigDecimal;

/**
 * Resultado da comparação de preços entre dois listings.
 * Cumpre as regras:
 * 1. "Calcule variação de preço apenas entre observações com janela temporal comparável"
 * 2. "Não inferir preço final com frete não conhecido"
 */
public record PriceComparisonResult(
        BigDecimal basePrice,
        BigDecimal targetPrice,
        BigDecimal priceDifference,
        BigDecimal priceVariationPercentage,
        boolean isTemporalWindowComparable,
        Long timeDeltaDays,
        TemporalComparabilityStatus temporalStatus,
        String temporalWarningMessage,
        BigDecimal baseShippingCost,
        BigDecimal targetShippingCost,
        ShippingStatus baseShippingStatus,
        ShippingStatus targetShippingStatus,
        BigDecimal baseFinalPriceWithShipping,
        BigDecimal targetFinalPriceWithShipping,
        BigDecimal totalPriceDifferenceWithShipping,
        BigDecimal totalPriceVariationPercentageWithShipping,
        String shippingNotice
) {
}
