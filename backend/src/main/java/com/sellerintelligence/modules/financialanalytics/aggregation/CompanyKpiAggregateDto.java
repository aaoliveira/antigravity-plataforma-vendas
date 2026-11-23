package com.sellerintelligence.modules.financialanalytics.aggregation;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Agregação analítica consolidada por Empresa / Unidade de Negócio.
 */
public record CompanyKpiAggregateDto(
        UUID companyId,
        String corporateName,
        long orderCount,
        BigDecimal grossRevenue,
        BigDecimal netRevenue,
        BigDecimal contributionMarginAmount,
        BigDecimal contributionMarginRatio,
        BigDecimal revenueSharePercent
) {}
