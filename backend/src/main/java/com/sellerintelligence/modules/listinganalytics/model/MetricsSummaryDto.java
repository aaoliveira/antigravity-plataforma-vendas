package com.sellerintelligence.modules.listinganalytics.model;

import java.math.BigDecimal;

/**
 * Resumo consolidado de métricas e taxas de desempenho no período filtrado.
 */
public record MetricsSummaryDto(
        long totalMetricsRecorded,
        long availableMetricsCount,
        long unavailableMetricsCount,
        long insufficientDataCount,
        long unsupportedByChannelCount,
        BigDecimal totalVisits,
        BigDecimal totalOrders,
        BigDecimal totalUnitsSold,
        BigDecimal totalRevenue,
        BigDecimal totalClicks,
        BigDecimal totalImpressions,
        BigDecimal overallConversionRateOrders,
        BigDecimal overallCtr,
        String antiHallucinationNotice
) {
}
