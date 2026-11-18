package com.sellerintelligence.modules.competitorresearch.comparison.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Ponto individual verificado no histórico de preços.
 */
public record ListingPricePointDto(
        UUID id,
        BigDecimal price,
        BigDecimal shippingCost,
        ShippingStatus shippingStatus,
        BigDecimal finalPriceWithShipping,
        Instant observedAt,
        Instant collectedAt,
        String sourceType,
        String sourceUrl
) {
}
