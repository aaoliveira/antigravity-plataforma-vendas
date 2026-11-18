package com.sellerintelligence.modules.productmonitoring.model;

import java.math.BigDecimal;

public record TriggerSnapshotRequest(
        BigDecimal price,
        BigDecimal shippingCost,
        Integer availableQuantity,
        String status,
        String note
) {
}
