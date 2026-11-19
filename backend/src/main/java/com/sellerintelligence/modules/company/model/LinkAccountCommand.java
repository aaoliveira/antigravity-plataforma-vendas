package com.sellerintelligence.modules.company.model;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Comando para vincular explicitamente uma conta de marketplace a uma empresa.
 */
public record LinkAccountCommand(
        @NotNull(message = "sellerAccountId é obrigatório")
        UUID sellerAccountId,

        String notes,

        boolean replaceActiveLink
) {}
