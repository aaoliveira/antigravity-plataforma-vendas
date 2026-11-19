package com.sellerintelligence.modules.pricing.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado da resolução de regras de cobrança para um marketplace, modalidade e faixa de preço.
 * Explicita ausência de taxas sem inventar valores zerados (Anti-Alucinação).
 */
public record FeeResolutionResult(
        String marketplace,
        String listingType,
        String categoryId,
        BigDecimal price,
        String currency,
        List<ResolvedFeeItem> resolvedFees,
        BigDecimal totalFeeAmount,
        boolean hasMissingFees,
        List<FeeType> missingFeeTypes,
        FeeResolutionStatus resolutionStatus,
        String userActionRequired
) {
}
