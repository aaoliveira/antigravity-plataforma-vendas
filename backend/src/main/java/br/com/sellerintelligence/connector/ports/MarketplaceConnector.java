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
}
