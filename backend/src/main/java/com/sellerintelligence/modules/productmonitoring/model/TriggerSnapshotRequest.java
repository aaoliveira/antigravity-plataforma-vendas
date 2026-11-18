package com.sellerintelligence.modules.productmonitoring.model;

import java.math.BigDecimal;
import java.time.Instant;

public record TriggerSnapshotRequest(
        BigDecimal price,
        BigDecimal shippingCost,
        Integer availableQuantity,
        String status,
        String note,
        String idempotencyKey,
        String sourceUrl,
        String sourceType,
        Instant validUntil,
        Boolean simulateSourceUnavailable
) {
    public TriggerSnapshotRequest(
            BigDecimal price,
            BigDecimal shippingCost,
            Integer availableQuantity,
            String status,
            String note
    ) {
        this(price, shippingCost, availableQuantity, status, note, null, null, null, null, false);
    }
}
