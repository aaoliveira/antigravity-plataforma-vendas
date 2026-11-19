package com.sellerintelligence.modules.pricing.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Comando para registrar ou atualizar o custo de um produto/variante/empresa.
 */
public record RegisterProductCostCommand(
        @NotNull(message = "productId é obrigatório")
        UUID productId,

        UUID variantId,

        UUID companyId,

        String currency,

        String unit,

        @NotNull(message = "acquisitionCost é obrigatório")
        @DecimalMin(value = "0.0", message = "acquisitionCost não pode ser negativo")
        BigDecimal acquisitionCost,

        @DecimalMin(value = "0.0", message = "packagingCost não pode ser negativo")
        BigDecimal packagingCost,

        @DecimalMin(value = "0.0", message = "additionalCost não pode ser negativo")
        BigDecimal additionalCost,

        @DecimalMin(value = "0.0", message = "subsidizedShippingCost não pode ser negativo")
        BigDecimal subsidizedShippingCost,

        @DecimalMin(value = "0.0", message = "taxRatePercent não pode ser negativo")
        BigDecimal taxRatePercent,

        @DecimalMin(value = "0.0", message = "otherExpensesPercent não pode ser negativo")
        BigDecimal otherExpensesPercent,

        Instant effectiveFrom,

        String notes
) {
}
