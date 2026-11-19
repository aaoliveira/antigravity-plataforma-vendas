package com.sellerintelligence.modules.financialanalytics.model;

import java.math.BigDecimal;

/**
 * Resumo agregado dos fatos financeiros de múltiplos pedidos para dashboards e relatórios.
 * Contém o disclaimer anti-alucinação proibindo categoricamente o rótulo de Lucro Líquido.
 */
public record FinancialFactSummaryDto(
        long totalOrders,
        BigDecimal totalGrossRevenue,
        BigDecimal totalDiscountAmount,
        BigDecimal totalNetRevenue,
        BigDecimal totalCancellationAmount,
        BigDecimal totalReturnAmount,
        BigDecimal totalMarketplaceFees,
        BigDecimal totalFixedFees,
        BigDecimal totalShippingCost,
        BigDecimal totalProductCost,
        BigDecimal totalPackagingCost,
        BigDecimal totalAdditionalCost,
        BigDecimal totalTaxAmount,
        BigDecimal totalOtherExpensesAmount,
        BigDecimal totalDeductions,
        BigDecimal totalContributionMarginAmount,
        BigDecimal averageContributionMarginRatio,
        long reconciledCount,
        long estimatedCount,
        long divergentCount,
        long ordersWithMissingDataCount,
        String antiHallucinationDisclaimer
) {}
