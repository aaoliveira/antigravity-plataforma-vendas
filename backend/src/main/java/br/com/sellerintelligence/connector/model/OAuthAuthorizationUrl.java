package br.com.sellerintelligence.connector.model;

import java.time.OffsetDateTime;

public record OAuthAuthorizationUrl(
        String authorizationUrl,
        String state,
        OffsetDateTime expiresAt
) {
}
