package com.sellerintelligence.modules.pricing.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Requisição para simular margem integrando custo efetivo cadastrado do produto e regras ativas de taxas do canal.
 */
public record ProductMarginSimulationRequest(
        @NotNull(message = "productId é obrigatório")
        UUID productId,

        UUID variantId,
        UUID companyId,

        @NotBlank(message = "marketplace é obrigatório")
        String marketplace,

        @NotNull(message = "grossSellingPrice é obrigatório")
        @DecimalMin(value = "0.01", message = "Preço de venda deve ser maior que zero")
        BigDecimal grossSellingPrice,

        @DecimalMin(value = "0.00", message = "Desconto promocional não pode ser negativo")
        BigDecimal promotionalDiscount,

        String listingType,
        String category,
        Instant referenceDate,

        BigDecimal minMarginPercent,
        BigDecimal targetMarginPercent,
        BigDecimal targetMarginAmount
) {}
