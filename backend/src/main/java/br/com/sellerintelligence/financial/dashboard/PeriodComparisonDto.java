package br.com.sellerintelligence.financial.dashboard;

import java.math.BigDecimal;

public record PeriodComparisonDto(
        FinancialSummaryDto current,
        FinancialSummaryDto previous,
        BigDecimal revenueDeltaPercentage,
        BigDecimal ordersDeltaPercentage,
        BigDecimal marginDeltaPercentage,
        boolean isPreviousIncomplete
) {}
