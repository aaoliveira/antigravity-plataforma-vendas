package com.sellerintelligence.modules.productmonitoring.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MonitoredItemDto(
        UUID id,
        UUID groupId,
        String marketplace,
        String sourcePolicyId,
        String externalListingId,
        String title,
        BigDecimal initialPrice,
        BigDecimal currentPrice,
        String currency,
        String sourceUrl,
        String sellerName,
        String itemCondition,
        MonitoredItemStatus status,
        int monitoringIntervalMinutes,
        Instant lastSnapshotAt,
        Instant nextRunAt,
        Instant firstSnapshotAt,
        int totalSnapshots,
        Instant createdAt
) {
}
