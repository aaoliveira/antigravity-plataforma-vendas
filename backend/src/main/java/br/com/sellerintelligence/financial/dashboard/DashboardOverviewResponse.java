package br.com.sellerintelligence.financial.dashboard;

import br.com.sellerintelligence.sync.domain.DataProvenance;

import java.util.List;

public record DashboardOverviewResponse(
        FinancialSummaryDto summary,
        PeriodComparisonDto comparison,
        List<ProductProfitabilityDto> byProduct,
        List<StoreProfitabilityDto> byStore,
        DataProvenance provenance,
        String dataNotice
) {}
