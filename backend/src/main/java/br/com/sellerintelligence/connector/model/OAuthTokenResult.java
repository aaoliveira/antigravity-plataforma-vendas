package br.com.sellerintelligence.connector.model;

import java.time.OffsetDateTime;

public record OAuthTokenResult(
        String accessToken,
        String refreshToken,
        String externalAccountId,
        String scopesConsented,
        OffsetDateTime expiresAt,
        OffsetDateTime refreshExpiresAt,
        String environment
) {
}
