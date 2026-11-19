package com.sellerintelligence.modules.pricing.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record FeeResolutionRequest(
        @NotBlank(message = "marketplace é obrigatório")
        String marketplace,

        @NotBlank(message = "listingType é obrigatório")
        String listingType,

        String categoryId,

        @NotNull(message = "price é obrigatório")
        @DecimalMin(value = "0.0", message = "price não pode ser negativo")
        BigDecimal price,

        Instant referenceDate
) {
}
