package com.sellerintelligence.modules.competitorresearch.comparison.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Decomposição transparente e explicável do score de equivalência entre dois listings.
 * Cumpre a regra de aceite: "score e critérios explicáveis; itens incompatíveis são sinalizados".
 */
public record EquivalenceScoreBreakdown(
        BigDecimal gtinScore,
        BigDecimal brandScore,
        BigDecimal modelScore,
        BigDecimal titleScore,
        BigDecimal attributesScore,
        BigDecimal overallScore,
        BigDecimal thresholdApplied,
        boolean isEquivalent,
        boolean isCompatible,
        CompatibilityStatus compatibilityStatus,
        List<String> incompatibilityReasons,
        String warningMessage,
        String explanation
) {
    public EquivalenceScoreBreakdown {
        if (incompatibilityReasons == null) {
            incompatibilityReasons = Collections.emptyList();
        }
        // Regra de Ouro: itens incompatíveis NUNCA podem ser dados como equivalentes
        if (!isCompatible) {
            isEquivalent = false;
        }
    }
}
