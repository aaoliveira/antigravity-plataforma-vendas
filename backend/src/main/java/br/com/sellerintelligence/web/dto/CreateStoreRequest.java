package br.com.sellerintelligence.web.dto;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateStoreRequest(
        @NotNull(message = "Company ID is required")
        UUID companyId,

        @NotNull(message = "Marketplace is required")
        Marketplace marketplace,

        @NotBlank(message = "Internal name is required")
        @Size(min = 2, max = 255)
        String internalName,

        String metadata
) {
}
