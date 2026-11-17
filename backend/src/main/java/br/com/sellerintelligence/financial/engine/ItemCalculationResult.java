package br.com.sellerintelligence.financial.engine;

import br.com.sellerintelligence.financial.domain.CalculationQuality;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemCalculationResult(
        UUID orderItemId,
        String sku,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal totalPrice,
        BigDecimal unitCogs,
        BigDecimal totalCogs,
        BigDecimal marketplaceFee,
        BigDecimal shippingSeller,
        BigDecimal taxValue,
        BigDecimal packagingCost,
        BigDecimal otherCosts,
        BigDecimal contributionMarginValue,
        BigDecimal contributionMarginPercentage,
        CalculationQuality quality,
        CalculationMemory memory
) {}
