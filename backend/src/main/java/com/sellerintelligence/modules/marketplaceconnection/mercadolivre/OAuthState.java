package com.sellerintelligence.modules.marketplaceconnection.mercadolivre;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Representação imutável do state anti-CSRF associado à sessão e parâmetros PKCE.
 */
public record OAuthState(
        String stateToken,
        UUID tenantId,
        UUID userId,
        String codeVerifier,
        Instant createdAt,
        Instant expiresAt
) {
    public OAuthState {
        Objects.requireNonNull(stateToken, "stateToken não pode ser nulo");
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(codeVerifier, "codeVerifier não pode ser nulo");
        Objects.requireNonNull(createdAt, "createdAt não pode ser nulo");
        Objects.requireNonNull(expiresAt, "expiresAt não pode ser nulo");
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
