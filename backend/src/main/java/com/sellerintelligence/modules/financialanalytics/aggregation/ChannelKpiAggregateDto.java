package com.sellerintelligence.modules.financialanalytics.aggregation;

import java.math.BigDecimal;

/**
 * Agregação analítica comparativa por Marketplace / Canal.
 */
public record ChannelKpiAggregateDto(
        String marketplace,
        long orderCount,
        BigDecimal grossRevenue,
        BigDecimal netRevenue,
        BigDecimal totalDeductions,
        BigDecimal contributionMarginAmount,
        BigDecimal contributionMarginRatio,
        BigDecimal averageOrderValue,
        BigDecimal revenueSharePercent
) {}
