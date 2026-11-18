package com.sellerintelligence.modules.marketplaceconnection.dto;

import java.time.Instant;

/**
 * DTO seguro para iniciação de fluxo de conexão de marketplace.
 */
public record InitiateAuthResponse(
        String authorizationUrl,
        String stateOrSignature,
        Instant expiresAt
) {
}
