package com.sellerintelligence.modules.marketplaceconnection.dto;

import com.sellerintelligence.modules.marketplaceconnection.MarketplaceConnection;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO seguro e universal de resumo de conexão com marketplaces (Mercado Livre, Shopee, etc.).
 * Omite categoricamente tokens de acesso, refresh tokens ou qualquer segredo criptográfico.
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
