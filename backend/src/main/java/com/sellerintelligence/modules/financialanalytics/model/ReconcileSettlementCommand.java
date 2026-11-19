package com.sellerintelligence.modules.financialanalytics.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Comando para reconciliar os fatos financeiros de um pedido com o extrato/liquidação externa do marketplace.
 */
public record ReconcileSettlementCommand(
        @NotBlank(message = "settlementExternalId é obrigatório")
        String settlementExternalId,

        @NotNull(message = "settlementDate é obrigatório")
        Instant settlementDate,

        BigDecimal confirmedMarketplaceFee,

        BigDecimal confirmedFixedFee,

        BigDecimal confirmedShippingCost,

        BigDecimal payoutAmount,

        String notes
) {}
