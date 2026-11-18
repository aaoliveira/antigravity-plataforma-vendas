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
        boolean isEffectiveSnapshot
) {
}
