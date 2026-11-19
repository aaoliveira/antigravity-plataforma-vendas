package com.sellerintelligence.modules.company.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Comando para criação de nova Empresa / Unidade de Negócio.
 */
public record CreateCompanyCommand(
        @NotBlank(message = "Razão social (corporateName) é obrigatória")
        @Size(max = 255, message = "Razão social não pode exceder 255 caracteres")
        String corporateName,

        @Size(max = 255, message = "Nome fantasia não pode exceder 255 caracteres")
        String tradeName,

        @NotBlank(message = "CNPJ é obrigatório")
        String cnpj,

        @Size(max = 50, message = "Inscrição estadual não pode exceder 50 caracteres")
        String stateRegistration,

        @Size(max = 50, message = "Regime tributário não pode exceder 50 caracteres")
        String taxRegime,

        String notes
) {}
