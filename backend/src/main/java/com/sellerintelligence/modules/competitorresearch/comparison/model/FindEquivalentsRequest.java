package com.sellerintelligence.modules.competitorresearch.comparison.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Requisição para buscar e classificar anúncios equivalentes para um item base.
 */
public record FindEquivalentsRequest(
        ComparableListingItem baseListing,
        String baseListingId,
        List<ComparableListingItem> candidates,
        BigDecimal threshold,
        Integer maxComparableDays
) {
    public FindEquivalentsRequest {
        if (candidates == null) {
            candidates = Collections.emptyList();
        }
        if (threshold == null) {
            threshold = new BigDecimal("0.7500");
        }
        if (maxComparableDays == null) {
            maxComparableDays = 30;
        }
    }
}
