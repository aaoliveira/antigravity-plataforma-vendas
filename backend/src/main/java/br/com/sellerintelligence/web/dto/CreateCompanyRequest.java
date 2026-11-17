package br.com.sellerintelligence.web.dto;

import br.com.sellerintelligence.tenancy.company.TaxRegime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCompanyRequest(
        @NotBlank(message = "CNPJ is required")
        String cnpj,

        @NotBlank(message = "Trade name (Nome Fantasia) is required")
        @Size(min = 2, max = 255)
        String tradeName,

        @Size(max = 255)
        String legalName,

        @NotNull(message = "Tax regime is required")
        TaxRegime taxRegime
) {
}
