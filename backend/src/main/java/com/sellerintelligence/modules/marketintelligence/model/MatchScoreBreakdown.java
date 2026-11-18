package com.sellerintelligence.modules.marketintelligence.model;

import java.math.BigDecimal;

/**
 * Detalhamento do score multi-fator de correspondência com explicabilidade.
 */
public record MatchScoreBreakdown(
        BigDecimal overallScore,
        BigDecimal gtinScore,
        BigDecimal titleScore,
        BigDecimal brandScore,
        BigDecimal priceScore,
        MatchStatus status,
        String explanation,
        BigDecimal thresholdApplied
) {
}
