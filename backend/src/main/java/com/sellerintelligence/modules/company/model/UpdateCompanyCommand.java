package com.sellerintelligence.modules.company.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Comando para atualização dos dados cadastrais de Empresa.
 */
public record UpdateCompanyCommand(
        @NotBlank(message = "Razão social (corporateName) é obrigatória")
        @Size(max = 255, message = "Razão social não pode exceder 255 caracteres")
        String corporateName,

        @Size(max = 255, message = "Nome fantasia não pode exceder 255 caracteres")
        String tradeName,

        @Size(max = 50, message = "Inscrição estadual não pode exceder 50 caracteres")
        String stateRegistration,

        @Size(max = 50, message = "Regime tributário não pode exceder 50 caracteres")
        String taxRegime,

        Boolean isActive,

        String notes
) {}
