package com.sellerintelligence.modules.competitorresearch.comparison.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Requisição para registrar um ponto legítimo de preço de um anúncio.
 */
public record RecordPricePointRequest(
        @NotBlank String marketplace,
        @NotBlank String externalListingId,
        @NotBlank String title,
        @NotNull BigDecimal price,
        BigDecimal shippingCost,
        Instant observedAt,
        String sourceType,
        String sourceUrl
) {
    public RecordPricePointRequest {
        if (observedAt == null) {
            observedAt = Instant.now();
        }
        if (sourceType == null || sourceType.isBlank()) {
            sourceType = "OFFICIAL";
        }
    }
}
