package com.sellerintelligence.modules.competitorresearch.comparison.model;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Requisição para comparar dois listings (por objeto direto ou IDs de anúncio).
 */
public record CompareListingsRequest(
        ComparableListingItem baseListing,
        ComparableListingItem targetListing,
        String baseListingId,
        String targetListingId,
        BigDecimal threshold,
        Integer maxComparableDays,
        Boolean persistComparison
) {
    public CompareListingsRequest {
        if (threshold == null) {
            threshold = new BigDecimal("0.7500");
        }
        if (maxComparableDays == null) {
            maxComparableDays = 30;
        }
        if (persistComparison == null) {
            persistComparison = false;
        }
    }
}
