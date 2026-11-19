package com.sellerintelligence.modules.pricing.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Resultado formal do cálculo determinístico de precificação e margem de contribuição.
 * NOTA: contributionMarginAmount e contributionMarginPercent NÃO representam Lucro Líquido.
 */
public record PricingCalculationResult(
        BigDecimal grossSellingPrice,
        BigDecimal promotionalDiscount,
        BigDecimal netSellingPrice,
        BigDecimal totalDirectCosts,
        BigDecimal totalVariableDeductions,
        BigDecimal totalFixedDeductions,
        BigDecimal totalDeductions,
        BigDecimal contributionMarginAmount,
        BigDecimal contributionMarginPercent,
        BigDecimal breakEvenPrice,
        boolean isBreakEvenAttainable,
        BigDecimal minPrice,
        boolean isMinPriceAttainable,
        BigDecimal targetPrice,
        boolean isTargetPriceAttainable,
        String currency,
        String formulaVersion,
        Instant calculatedAt,
        List<PricingDeductionItem> deductionItems,
        List<String> assumptions,
        List<String> warnings
) {}
