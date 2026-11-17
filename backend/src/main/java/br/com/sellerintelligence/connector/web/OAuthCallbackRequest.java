package br.com.sellerintelligence.connector.web;

import jakarta.validation.constraints.NotBlank;

public record OAuthCallbackRequest(
        @NotBlank(message = "State token is required")
        String state,

        @NotBlank(message = "Authorization code is required")
        String code
) {
}
