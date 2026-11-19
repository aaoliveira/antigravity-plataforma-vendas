package com.sellerintelligence.modules.financialanalytics.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO com dados dos fatos financeiros de um item de pedido.
 */
public record OrderItemFinancialFactDto(
        UUID id,
        UUID orderItemId,
        UUID productId,
        UUID variantId,
        String sku,
        String title,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal grossRevenue,
        BigDecimal discountAmount,
        BigDecimal netRevenue,
        BigDecimal cancellationAmount,
        BigDecimal returnAmount,
        BigDecimal marketplaceFee,
        BigDecimal fixedFee,
        BigDecimal shippingCost,
        BigDecimal unitProductCost,
        BigDecimal productCost,
        BigDecimal packagingCost,
        BigDecimal additionalCost,
        BigDecimal taxRatePercent,
        BigDecimal taxAmount,
        BigDecimal otherExpensesAmount,
        BigDecimal totalDeductions,
        BigDecimal contributionMarginAmount,
        BigDecimal contributionMarginRatio,
        FinancialValueProvenance costProvenance,
        boolean hasMissingData,
        String missingDataReasons
) {}
