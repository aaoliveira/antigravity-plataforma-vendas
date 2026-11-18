package com.sellerintelligence.modules.identityaccess.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SwitchTenantRequest(
        @NotNull(message = "O identificador do tenant destino (targetTenantId) é obrigatório")
        UUID targetTenantId
) {
}
