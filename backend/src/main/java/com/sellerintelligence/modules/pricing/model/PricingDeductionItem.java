package com.sellerintelligence.modules.pricing.model;

import java.math.BigDecimal;

/**
 * Detalhamento de cada parcela deduzida da receita para apuração da margem de contribuição.
 */
public record PricingDeductionItem(
        String name,
        DeductionCategory category,
        BigDecimal amount,
        BigDecimal percentageOfNetRevenue,
        BigDecimal appliedRate,
        boolean isPercentage
) {}
