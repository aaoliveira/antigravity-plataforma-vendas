package br.com.sellerintelligence.sync.web;

import java.time.Instant;

public record SyncStatsResponse(
        long totalProducts,
        long totalListings,
        long totalOrders,
        Instant lastSyncAt
) {}
