package com.sellerintelligence.modules.financialanalytics.aggregation;

import java.time.Instant;
import java.util.UUID;

/**
 * Filtros combináveis para consultas analíticas agregadas.
 */
public record AggregatedKpiFilter(
        UUID companyId,
        String marketplace,
        UUID sellerAccountId,
        Instant startDate,
        Instant endDate
) {
    public String cacheKeySuffix() {
        return String.format("c_%s_m_%s_a_%s_s_%s_e_%s",
                companyId != null ? companyId.toString() : "all",
                marketplace != null ? marketplace.trim().toLowerCase() : "all",
                sellerAccountId != null ? sellerAccountId.toString() : "all",
                startDate != null ? startDate.toString() : "none",
                endDate != null ? endDate.toString() : "none"
        );
    }
}
