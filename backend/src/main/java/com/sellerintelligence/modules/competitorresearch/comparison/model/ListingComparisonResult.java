package com.sellerintelligence.modules.competitorresearch.comparison.model;

import java.time.Instant;

/**
 * Resultado completo e unificado de comparação entre dois listings.
 */
public record ListingComparisonResult(
        ComparableListingItem baseListing,
        ComparableListingItem targetListing,
        EquivalenceScoreBreakdown similarity,
        PriceComparisonResult priceComparison,
        Instant comparedAt
) {
}
