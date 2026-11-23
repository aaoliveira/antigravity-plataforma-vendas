package com.sellerintelligence.modules.financialanalytics.aggregation;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Agregação analítica periódica (diária, semanal, mensal) para gráficos de tendência.
 */
public record PeriodicKpiAggregateDto(
        String periodKey,
        Instant periodStart,
        Instant periodEnd,
        long orderCount,
        BigDecimal grossRevenue,
        BigDecimal netRevenue,
        BigDecimal totalDeductions,
        BigDecimal contributionMarginAmount,
        BigDecimal contributionMarginRatio
) {}
