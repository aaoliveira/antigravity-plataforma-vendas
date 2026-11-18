package com.sellerintelligence.modules.marketintelligence.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payload de entrada para registro/ingestão de observação de mercado.
 */
public record CreateObservationRequest(
        @NotBlank(message = "observedListingId é obrigatório")
        @Size(max = 100, message = "observedListingId não pode exceder 100 caracteres")
        String observedListingId,

        @NotBlank(message = "marketplace é obrigatório")
        @Size(max = 50, message = "marketplace não pode exceder 50 caracteres")
        String marketplace,

        @NotBlank(message = "title é obrigatório")
        @Size(max = 255, message = "title não pode exceder 255 caracteres")
        String title,

        @NotNull(message = "price é obrigatório")
        @DecimalMin(value = "0.0001", message = "price deve ser maior que zero")
        BigDecimal price,

        @Size(max = 10, message = "currency não pode exceder 10 caracteres")
        String currency,

        ItemCondition itemCondition,

        @Size(max = 255, message = "sellerIdentifier não pode exceder 255 caracteres")
        String sellerIdentifier,

        @NotNull(message = "sourceType é obrigatório")
        MarketObservationSourceType sourceType,

        @NotBlank(message = "sourceUrl é obrigatória")
        @Size(max = 1000, message = "sourceUrl não pode exceder 1000 caracteres")
        String sourceUrl,

        Instant collectedAt,
        Instant observedAt,
        Instant validUntil,

        @NotBlank(message = "method é obrigatório")
        @Size(max = 100, message = "method não pode exceder 100 caracteres")
        String method,

        @DecimalMin(value = "0.0000", message = "confidence deve ser entre 0 e 1")
        @DecimalMax(value = "1.0000", message = "confidence deve ser entre 0 e 1")
        BigDecimal confidence,

        String methodologyDescription,
        String modelVersion,
        String timeWindow,

        @Size(max = 50, message = "gtin não pode exceder 50 caracteres")
        String gtin,

        @Size(max = 100, message = "brand não pode exceder 100 caracteres")
        String brand,

        BigDecimal salesVolume,
        Long visitsCount,
        BigDecimal ctr,
        BigDecimal revenue,
        String rawPayload
) {
}
