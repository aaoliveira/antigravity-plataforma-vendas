package br.com.sellerintelligence.financial.web;

import br.com.sellerintelligence.financial.domain.SkuCost;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SkuCostResponse(
        UUID id,
        String sku,
        BigDecimal costPrice,
        String currency,
        String unit,
        String origin,
        Instant effectiveFrom,
        Instant effectiveTo,
        String notes
) {
    public static SkuCostResponse fromDomain(SkuCost c) {
        return new SkuCostResponse(
                c.getId(),
                c.getSku(),
                c.getCostPrice(),
                c.getCurrency(),
                c.getUnit(),
                c.getOrigin(),
                c.getEffectiveFrom(),
                c.getEffectiveTo(),
                c.getNotes()
        );
    }
}
