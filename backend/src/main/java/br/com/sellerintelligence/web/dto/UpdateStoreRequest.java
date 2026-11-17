package br.com.sellerintelligence.web.dto;

import br.com.sellerintelligence.tenancy.store.ConnectionStatus;
import jakarta.validation.constraints.Size;

public record UpdateStoreRequest(
        @Size(min = 2, max = 255)
        String internalName,

        String externalAccountId,

        ConnectionStatus connectionStatus,

        String metadata
) {
}
