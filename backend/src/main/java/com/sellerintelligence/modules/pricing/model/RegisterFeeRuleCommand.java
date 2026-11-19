package com.sellerintelligence.modules.pricing.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Comando para registrar ou atualizar regra de tarifa ou comissão em marketplace.
 */
public record RegisterFeeRuleCommand(
        @NotBlank(message = "marketplace é obrigatório")
        String marketplace,

        @NotBlank(message = "listingType é obrigatório")
        String listingType,

        String categoryId,

        @NotNull(message = "feeType é obrigatório")
        FeeType feeType,

        @NotNull(message = "feeValueType é obrigatório")
        FeeValueType feeValueType,

        BigDecimal percentageRate,

        BigDecimal fixedAmount,

        String currency,

        BigDecimal minPriceThreshold,

        BigDecimal maxPriceThreshold,

        @NotNull(message = "sourceType é obrigatório")
        FeeSourceType sourceType,

        @NotBlank(message = "sourceReference é obrigatório para conformidade anti-alucinação")
        String sourceReference,

        String legalDisclaimer,

        Instant effectiveFrom
) {
}
