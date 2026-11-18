package com.sellerintelligence.modules.productmonitoring.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MonitoredItemSnapshotDto(
        UUID id,
        UUID itemId,
        UUID groupId,
        String marketplace,
        String externalListingId,
        BigDecimal price,
        String currency,
        BigDecimal shippingCost,
        String shippingStatus,
        Integer availableQuantity,
        String status,
        String provenanceType,
        Instant collectedAt,
        Instant observedAt,
        Instant validUntil,
        boolean isEffectiveSnapshot,
        String idempotencyKey,
        String sourceUrl,
        String sourceType,
        String collectionStatus,
        UUID previousSnapshotId,
        boolean hasPriceChange,
        BigDecimal priceChangeAmount,
        BigDecimal priceChangePercent,
        boolean hasAvailabilityChange,
        boolean hasAttributeChange,
        String changeType
) {
    public MonitoredItemSnapshotDto(
            UUID id,
            UUID itemId,
            UUID groupId,
            String marketplace,
            String externalListingId,
            BigDecimal price,
            String currency,
            BigDecimal shippingCost,
            String shippingStatus,
            Integer availableQuantity,
            String status,
            String provenanceType,
            Instant collectedAt,
            Instant observedAt,
            boolean isEffectiveSnapshot
    ) {
        this(
                id, itemId, groupId, marketplace, externalListingId,
                price, currency, shippingCost, shippingStatus,
                availableQuantity, status, provenanceType,
                collectedAt, observedAt, null, isEffectiveSnapshot,
                null, null, "OFFICIAL_API", "SUCCESS",
                null, false, BigDecimal.ZERO, BigDecimal.ZERO,
                false, false, "INITIAL"
        );
    }
}
