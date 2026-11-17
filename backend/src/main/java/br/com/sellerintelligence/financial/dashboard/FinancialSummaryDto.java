package br.com.sellerintelligence.financial.dashboard;

import java.math.BigDecimal;
import java.time.Instant;

public record FinancialSummaryDto(
        BigDecimal grossRevenue,
        BigDecimal netRevenue,
        long orderCount,
        BigDecimal averageTicket,
        BigDecimal totalCogs,
        BigDecimal totalMarketplaceFees,
        BigDecimal totalShippingSeller,
        BigDecimal totalTaxes,
        BigDecimal totalPackaging,
        BigDecimal totalOtherCosts,
        BigDecimal contributionMarginValue,
        BigDecimal contributionMarginPercentage,
        long completedCalculationsCount,
        long partialCalculationsCount,
        BigDecimal calculationCoveragePercentage,
        Instant lastSyncedAt
) {}
