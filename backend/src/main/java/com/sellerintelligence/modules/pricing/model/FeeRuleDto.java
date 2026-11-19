package com.sellerintelligence.modules.pricing.model;

import com.sellerintelligence.modules.pricing.entity.FeeRuleRecord;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FeeRuleDto(
        UUID id,
        UUID tenantId,
        String marketplace,
        String listingType,
        String categoryId,
        FeeType feeType,
        FeeValueType feeValueType,
        BigDecimal percentageRate,
        BigDecimal fixedAmount,
        String currency,
        BigDecimal minPriceThreshold,
        BigDecimal maxPriceThreshold,
        FeeSourceType sourceType,
        String sourceReference,
        String legalDisclaimer,
        Instant effectiveFrom,
        Instant effectiveTo,
        Integer version,
        Boolean isActive,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static FeeRuleDto fromRecord(FeeRuleRecord record) {
        return new FeeRuleDto(
                record.getId(),
                record.getTenantId(),
                record.getMarketplace(),
                record.getListingType(),
                record.getCategoryId(),
                record.getFeeType(),
                record.getFeeValueType(),
                record.getPercentageRate(),
                record.getFixedAmount(),
                record.getCurrency(),
                record.getMinPriceThreshold(),
                record.getMaxPriceThreshold(),
                record.getSourceType(),
                record.getSourceReference(),
                record.getLegalDisclaimer(),
                record.getEffectiveFrom(),
                record.getEffectiveTo(),
                record.getVersion(),
                record.getIsActive(),
                record.getCreatedBy(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
