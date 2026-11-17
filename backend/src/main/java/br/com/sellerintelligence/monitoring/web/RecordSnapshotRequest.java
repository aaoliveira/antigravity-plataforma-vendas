package br.com.sellerintelligence.monitoring.web;

import java.math.BigDecimal;
import java.time.Instant;

public record RecordSnapshotRequest(
        BigDecimal price,
        BigDecimal originalPrice,
        String listingStatus,
        String title,
        String sellerNickname,
        String sellerReputation,
        Boolean freeShipping,
        String thumbnailUrl,
        String source,
        Instant collectedAt
) {}
