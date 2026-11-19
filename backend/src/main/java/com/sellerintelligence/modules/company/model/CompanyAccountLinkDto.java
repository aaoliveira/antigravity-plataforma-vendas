package com.sellerintelligence.modules.company.model;

import com.sellerintelligence.modules.company.entity.CompanyAccountLinkRecord;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO para transferência e apresentação de vínculo de conta com empresa.
 */
public record CompanyAccountLinkDto(
        UUID id,
        UUID tenantId,
        UUID companyId,
        String companyName,
        UUID sellerAccountId,
        String marketplace,
        String accountName,
        Instant effectiveFrom,
        Instant effectiveTo,
        boolean isActive,
        int version,
        String notes,
        String linkedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static CompanyAccountLinkDto fromRecord(
            CompanyAccountLinkRecord record,
            String companyName,
            String marketplace,
            String accountName
    ) {
        return new CompanyAccountLinkDto(
                record.getId(),
                record.getTenantId(),
                record.getCompanyId(),
                companyName,
                record.getSellerAccountId(),
                marketplace,
                accountName,
                record.getEffectiveFrom(),
                record.getEffectiveTo(),
                record.isActive(),
                record.getVersion(),
                record.getNotes(),
                record.getLinkedBy(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
