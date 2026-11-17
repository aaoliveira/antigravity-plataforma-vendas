package br.com.sellerintelligence.financial.dashboard;

import br.com.sellerintelligence.tenancy.store.Marketplace;

import java.time.Instant;
import java.util.UUID;

public record FinancialDashboardFilter(
        UUID companyId,
        UUID storeId,
        Marketplace marketplace,
        Instant from,
        Instant to,
        boolean compareWithPrevious
) {}
