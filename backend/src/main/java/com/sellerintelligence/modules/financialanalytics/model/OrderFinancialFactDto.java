package com.sellerintelligence.modules.financialanalytics.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO consolidado contendo os fatos financeiros do pedido, itens e auditoria.
 * Inclui obrigatoriamente o disclaimer anti-alucinação vedando o rótulo de Lucro Líquido.
 */
public record OrderFinancialFactDto(
        UUID id,
        UUID orderId,
        UUID sellerAccountId,
        UUID companyId,
        String marketplace,
        String externalOrderId,
        String orderStatus,
        Instant orderDate,
        BigDecimal grossRevenue,
        BigDecimal discountAmount,
        BigDecimal netRevenue,
        BigDecimal cancellationAmount,
        BigDecimal returnAmount,
        BigDecimal marketplaceFee,
        BigDecimal fixedFee,
        BigDecimal shippingCost,
        BigDecimal productCost,
        BigDecimal packagingCost,
        BigDecimal additionalCost,
        BigDecimal taxAmount,
        BigDecimal otherExpensesAmount,
        BigDecimal totalDeductions,
        BigDecimal contributionMarginAmount,
        BigDecimal contributionMarginRatio,
        String currency,
        ReconciliationStatus reconciliationStatus,
        FinancialValueProvenance feeProvenance,
        FinancialValueProvenance shippingProvenance,
        FinancialValueProvenance costProvenance,
        FinancialValueProvenance taxProvenance,
        boolean hasMissingData,
        String missingDataReasons,
        String calculationVersion,
        String calculationTraceJson,
        String settlementExternalId,
        Instant settlementDate,
        String settlementNotes,
        Instant reconciledAt,
        String antiHallucinationDisclaimer,
        List<OrderItemFinancialFactDto> items,
        Instant createdAt,
        Instant updatedAt
) {}
