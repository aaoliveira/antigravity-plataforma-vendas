package com.sellerintelligence.modules.pricing.model;

import java.math.BigDecimal;
import java.util.UUID;

public record ResolvedFeeItem(
        UUID ruleId,
        FeeType feeType,
        FeeValueType feeValueType,
        BigDecimal percentageRate,
        BigDecimal fixedAmount,
        BigDecimal calculatedAmount,
        String currency,
        FeeSourceType sourceType,
        String sourceReference,
        Integer ruleVersion
) {
}
