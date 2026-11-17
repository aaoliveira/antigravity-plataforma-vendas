package br.com.sellerintelligence.financial.engine;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemCalculationInput(
        UUID orderItemId,
        String sku,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal unitCogs,
        BigDecimal commissionPercentage,
        BigDecimal fixedFee,
        BigDecimal fixedFeeThreshold,
        BigDecimal shippingSeller,
        BigDecimal packagingCost,
        BigDecimal taxPercentage,
        BigDecimal otherVariablePercentage,
        BigDecimal otherVariableFixed
) {}
