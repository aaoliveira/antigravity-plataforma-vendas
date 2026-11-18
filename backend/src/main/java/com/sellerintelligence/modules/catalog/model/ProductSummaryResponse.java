package com.sellerintelligence.modules.catalog.model;

import java.util.UUID;

public record ProductSummaryResponse(
        UUID id,
        String title,
        String canonicalSku,
        String brand,
        String category,
        String status
) {}
