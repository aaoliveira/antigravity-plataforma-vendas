package br.com.sellerintelligence.connector.ports;

import br.com.sellerintelligence.connector.model.*;
import br.com.sellerintelligence.tenancy.store.Marketplace;

import java.util.List;

public interface MarketplaceConnector {

    Marketplace getMarketplace();

    List<ConnectorCapability> getCapabilities();

    default CapabilityStatus getCapabilityStatus(ConnectorCapabilityType type) {
        return getCapabilities().stream()
                .filter(c -> c.type() == type)
                .map(ConnectorCapability::status)
                .findFirst()
                .orElse(CapabilityStatus.UNSUPPORTED);
    }

    OAuthAuthorizationUrl generateAuthorizationUrl(String state, String redirectUri);

    OAuthTokenResult exchangeAuthorizationCode(String code, String redirectUri);

    OAuthTokenResult refreshAccessToken(String refreshToken);

    void revokeToken(String accessToken, String externalAccountId);

    boolean validateConnection(String accessToken, String externalAccountId);

    default ConnectorSyncBatch<ConnectorListing> fetchListings(String accessToken, String externalAccountId, String cursor, int limit) {
        return new ConnectorSyncBatch<>(List.of(), null, false);
    }

    default ConnectorSyncBatch<ConnectorOrder> fetchOrders(String accessToken, String externalAccountId, java.time.Instant from, String cursor, int limit) {
        return new ConnectorSyncBatch<>(List.of(), null, false);
    }
}
