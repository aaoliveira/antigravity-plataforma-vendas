package br.com.sellerintelligence.financial.dashboard;

import br.com.sellerintelligence.tenancy.store.Marketplace;

import java.math.BigDecimal;
import java.util.UUID;

public record StoreProfitabilityDto(
        UUID storeId,
        String storeName,
        Marketplace marketplace,
        long orderCount,
        BigDecimal grossRevenue,
        BigDecimal contributionMarginValue,
        BigDecimal contributionMarginPercentage
) {}
