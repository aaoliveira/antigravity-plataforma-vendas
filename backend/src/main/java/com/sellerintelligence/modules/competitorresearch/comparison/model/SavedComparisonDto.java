package com.sellerintelligence.modules.competitorresearch.comparison.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO para comparações salvas e auditadas no tenant.
 */
public record SavedComparisonDto(
        UUID id,
        UUID tenantId,
        String baseListingId,
        String baseTitle,
        String baseBrand,
        String baseMarketplace,
        BigDecimal basePrice,
        BigDecimal baseShippingCost,
        String targetListingId,
        String targetTitle,
        String targetBrand,
        String targetMarketplace,
        BigDecimal targetPrice,
        BigDecimal targetShippingCost,
        BigDecimal similarityScore,
        boolean isEquivalent,
        boolean isCompatible,
        CompatibilityStatus compatibilityStatus,
        List<String> incompatibilityReasons,
        boolean priceVariationComparable,
        BigDecimal priceDifference,
        BigDecimal priceVariationPercentage,
        Long timeDeltaDays,
        String explanation,
        Instant createdAt
) {
}
