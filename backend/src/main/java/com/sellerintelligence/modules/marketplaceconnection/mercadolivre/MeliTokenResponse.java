package com.sellerintelligence.modules.marketplaceconnection.mercadolivre;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Representação imutável da resposta do endpoint /oauth/token do Mercado Livre.
 */
public record MeliTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") Long expiresIn,
        @JsonProperty("scope") String scope,
        @JsonProperty("user_id") Object userId,
        @JsonProperty("refresh_token") String refreshToken
) {
    public MeliTokenResponse {
        Objects.requireNonNull(accessToken, "accessToken não pode ser nulo");
        Objects.requireNonNull(refreshToken, "refreshToken não pode ser nulo");
        if (expiresIn == null) {
            expiresIn = 21600L; // Padrão de 6 horas
        }
    }

    public String getFormattedUserId() {
        return userId != null ? String.valueOf(userId) : "unknown";
    }
}
