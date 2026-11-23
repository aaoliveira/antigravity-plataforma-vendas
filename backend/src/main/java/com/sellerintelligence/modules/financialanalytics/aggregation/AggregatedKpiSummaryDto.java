package com.sellerintelligence.modules.financialanalytics.aggregation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO de resumo analítico consolidado dos KPIs com catálogo de definições e proveniência.
 */
public record AggregatedKpiSummaryDto(
        UUID tenantId,
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
        BigDecimal averageOrderValue,
        long reconciledCount,
        long estimatedCount,
        long divergentCount,
        long ordersWithMissingDataCount,
        BigDecimal reconciliationRatePercent,
        boolean cached,
        Instant calculatedAt,
        String antiHallucinationDisclaimer,
        List<KpiDefinitionDto> kpiDefinitions
) {}
