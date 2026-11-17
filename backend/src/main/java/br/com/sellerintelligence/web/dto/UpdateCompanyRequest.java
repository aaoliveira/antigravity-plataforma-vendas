package br.com.sellerintelligence.web.dto;

import br.com.sellerintelligence.tenancy.company.TaxRegime;
import jakarta.validation.constraints.Size;

public record UpdateCompanyRequest(
        @Size(min = 2, max = 255)
        String tradeName,

        @Size(max = 255)
        String legalName,

        TaxRegime taxRegime
) {
}
