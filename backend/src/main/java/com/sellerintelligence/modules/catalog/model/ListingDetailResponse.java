package com.sellerintelligence.modules.catalog.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ListingDetailResponse(
        UUID id,
        UUID sellerAccountId,
        String sellerAccountName,
        String marketplace,
        String externalListingId,
        String externalSku,
        String externalVariationId,
        String title,
        BigDecimal price,
        String currency,
        Integer availableQuantity,
        String status,
        String permalink,
        String origin,
        String schemaVersion,
        Instant syncedAt,
        ProductSummaryResponse product,
        VariantSummaryResponse variant
) {}
