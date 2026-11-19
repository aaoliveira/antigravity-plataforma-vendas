package com.sellerintelligence.modules.pricing.model;

import com.sellerintelligence.modules.pricing.entity.ProductCostRecord;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductCostDto(
        UUID id,
        UUID tenantId,
        UUID productId,
        UUID variantId,
        UUID companyId,
        String currency,
        String unit,
        BigDecimal acquisitionCost,
        BigDecimal packagingCost,
        BigDecimal additionalCost,
        BigDecimal subsidizedShippingCost,
        BigDecimal totalDirectUnitCost,
        BigDecimal taxRatePercent,
        BigDecimal otherExpensesPercent,
        Instant effectiveFrom,
        Instant effectiveTo,
        Integer version,
        Boolean isActive,
        String createdBy,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductCostDto fromRecord(ProductCostRecord record) {
        return new ProductCostDto(
                record.getId(),
                record.getTenantId(),
                record.getProductId(),
                record.getVariantId(),
                record.getCompanyId(),
                record.getCurrency(),
                record.getUnit(),
                record.getAcquisitionCost(),
                record.getPackagingCost(),
                record.getAdditionalCost(),
                record.getSubsidizedShippingCost(),
                record.getTotalDirectUnitCost(),
                record.getTaxRatePercent(),
                record.getOtherExpensesPercent(),
                record.getEffectiveFrom(),
                record.getEffectiveTo(),
                record.getVersion(),
                record.getIsActive(),
                record.getCreatedBy(),
                record.getNotes(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
