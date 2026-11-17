package br.com.sellerintelligence.web.dto;

import br.com.sellerintelligence.tenancy.store.ConnectionStatus;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import br.com.sellerintelligence.tenancy.store.Store;
import br.com.sellerintelligence.tenancy.store.StoreStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StoreResponse(
        UUID id,
        UUID tenantId,
        UUID companyId,
        String companyTradeName,
        Marketplace marketplace,
        String internalName,
        String externalAccountId,
        ConnectionStatus connectionStatus,
        String metadata,
        StoreStatus status,
        OffsetDateTime createdAt
) {
    public static StoreResponse from(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getTenantId(),
                store.getCompany().getId(),
                store.getCompany().getTradeName(),
                store.getMarketplace(),
                store.getInternalName(),
                store.getExternalAccountId(),
                store.getConnectionStatus(),
                store.getMetadata(),
                store.getStatus(),
                store.getCreatedAt()
        );
    }
}
