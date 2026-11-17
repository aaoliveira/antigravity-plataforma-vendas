package br.com.sellerintelligence.connector.web;

import br.com.sellerintelligence.connector.model.ConnectorCapability;
import br.com.sellerintelligence.tenancy.store.ConnectionStatus;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import br.com.sellerintelligence.tenancy.store.Store;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ConnectionStatusResponse(
        UUID storeId,
        Marketplace marketplace,
        String internalName,
        ConnectionStatus connectionStatus,
        String externalAccountId,
        String environment,
        OffsetDateTime expiresAt,
        OffsetDateTime lastValidatedAt,
        List<ConnectorCapability> capabilities
) {
    public static ConnectionStatusResponse from(Store store, String environment,
                                                OffsetDateTime expiresAt, OffsetDateTime lastValidatedAt,
                                                List<ConnectorCapability> capabilities) {
        return new ConnectionStatusResponse(
                store.getId(),
                store.getMarketplace(),
                store.getInternalName(),
                store.getConnectionStatus(),
                store.getExternalAccountId(),
                environment,
                expiresAt,
                lastValidatedAt,
                capabilities
        );
    }
}
