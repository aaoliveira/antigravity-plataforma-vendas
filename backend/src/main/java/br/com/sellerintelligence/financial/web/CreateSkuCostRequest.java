package br.com.sellerintelligence.financial.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateSkuCostRequest(
        @NotBlank(message = "SKU é obrigatório")
        String sku,

        @NotNull(message = "Preço de custo é obrigatório")
        @PositiveOrZero(message = "Custo deve ser positivo ou zero")
        BigDecimal costPrice,

        String currency,
        String unit,
        String origin,
        Instant effectiveFrom,
        String notes
) {}
