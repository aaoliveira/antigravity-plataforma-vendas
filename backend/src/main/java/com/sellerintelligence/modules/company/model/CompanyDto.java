package com.sellerintelligence.modules.company.model;

import com.sellerintelligence.modules.company.entity.CompanyRecord;
import com.sellerintelligence.modules.company.util.CnpjValidator;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO para transferência e apresentação de dados de Empresa.
 */
public record CompanyDto(
        UUID id,
        UUID tenantId,
        String corporateName,
        String tradeName,
        String cnpj,
        String formattedCnpj,
        String stateRegistration,
        String taxRegime,
        boolean isActive,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static CompanyDto fromRecord(CompanyRecord record) {
        return new CompanyDto(
                record.getId(),
                record.getTenantId(),
                record.getCorporateName(),
                record.getTradeName(),
                record.getCnpj(),
                CnpjValidator.format(record.getCnpj()),
                record.getStateRegistration(),
                record.getTaxRegime(),
                record.isActive(),
                record.getNotes(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
