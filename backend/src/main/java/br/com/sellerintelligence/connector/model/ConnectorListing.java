package br.com.sellerintelligence.connector.model;

import java.math.BigDecimal;
import java.time.Instant;

public record ConnectorListing(
        String externalId,
        String sku,
        String title,
        String status,
        BigDecimal price,
        String currency,
        Integer availableQuantity,
        Integer soldQuantity,
        String listingType,
        String permalink,
        String thumbnailUrl,
        String rawPayload,
        Instant originUpdatedAt
) {}
