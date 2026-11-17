package br.com.sellerintelligence.connector.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record InitiateOAuthRequest(
        @NotNull(message = "Store ID is required")
        UUID storeId,

        @NotBlank(message = "Redirect URI is required")
        String redirectUri
) {
}
