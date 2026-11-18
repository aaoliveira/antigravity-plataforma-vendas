package com.sellerintelligence.modules.marketplaceconnection.mercadolivre.dto;

import java.time.Instant;

/**
 * DTO de resposta para iniciação de fluxo OAuth com PKCE.
 */
public record InitiateAuthResponse(
        String authorizationUrl,
        String stateToken,
        Instant expiresAt
) {
}
