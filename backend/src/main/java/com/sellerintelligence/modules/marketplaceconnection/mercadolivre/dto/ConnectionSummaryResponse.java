package com.sellerintelligence.modules.marketplaceconnection.mercadolivre.dto;

import com.sellerintelligence.modules.marketplaceconnection.MarketplaceConnection;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO seguro de resumo de conexão, omitindo qualquer dado sensível ou token em texto puro.
 */
public record ConnectionSummaryResponse(
        UUID id,
        UUID tenantId,
        String marketplace,
        String sellerAccountId,
        String accountName,
        String status,
        Instant tokenExpiresAt,
        String authorizedScopes,
        Instant lastRefreshedAt,
        Instant createdAt
) {
    public static ConnectionSummaryResponse fromEntity(MarketplaceConnection connection) {
        return new ConnectionSummaryResponse(
                connection.getId(),
                connection.getTenantId(),
                connection.getMarketplace(),
                connection.getSellerAccountId(),
                connection.getAccountName(),
                connection.getStatus(),
                connection.getTokenExpiresAt(),
                connection.getAuthorizedScopes(),
                connection.getLastRefreshedAt(),
                connection.getCreatedAt()
        );
    }
}
