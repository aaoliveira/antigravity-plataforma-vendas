package br.com.sellerintelligence.web.dto;

import br.com.sellerintelligence.tenancy.company.Company;
import br.com.sellerintelligence.tenancy.company.CompanyStatus;
import br.com.sellerintelligence.tenancy.company.TaxRegime;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CompanyResponse(
        UUID id,
        UUID tenantId,
        String cnpj,
        String tradeName,
        String legalName,
        TaxRegime taxRegime,
        CompanyStatus status,
        OffsetDateTime createdAt
) {
    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getTenantId(),
                company.getCnpj(),
                company.getTradeName(),
                company.getLegalName(),
                company.getTaxRegime(),
                company.getStatus(),
                company.getCreatedAt()
        );
    }
}
