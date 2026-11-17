package br.com.sellerintelligence.financial.dashboard;

import br.com.sellerintelligence.financial.domain.CalculationQuality;
import br.com.sellerintelligence.financial.domain.CalculationStatus;
import br.com.sellerintelligence.tenancy.store.Marketplace;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FinancialOrderListItemDto(
        UUID orderId,
        String externalOrderId,
        Instant orderDate,
        Marketplace marketplace,
        String storeName,
        int itemCount,
        BigDecimal grossRevenue,
        BigDecimal contributionMarginValue,
        BigDecimal contributionMarginPercentage,
        CalculationQuality calculationQuality,
        CalculationStatus calculationStatus
) {}
