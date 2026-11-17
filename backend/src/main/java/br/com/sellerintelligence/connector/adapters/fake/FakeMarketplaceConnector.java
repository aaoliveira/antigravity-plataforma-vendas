package br.com.sellerintelligence.connector.adapters.fake;

import br.com.sellerintelligence.connector.model.*;
import br.com.sellerintelligence.connector.ports.MarketplaceConnector;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Fake Marketplace Connector strictly restricted to the "test" profile.
 * Prohibited from being loaded in production.
 */
@Component
@Profile("test")
public class FakeMarketplaceConnector implements MarketplaceConnector {

    @Override
    public Marketplace getMarketplace() {
        return Marketplace.OUTRO;
    }

    @Override
    public List<ConnectorCapability> getCapabilities() {
        return List.of(
                ConnectorCapability.supported(ConnectorCapabilityType.CATALOG, "Fake catalog for integration testing"),
                ConnectorCapability.supported(ConnectorCapabilityType.ORDERS, "Fake orders for integration testing"),
                ConnectorCapability.unsupported(ConnectorCapabilityType.ADS, "Fake connector does not support ads")
        );
    }

    @Override
    public OAuthAuthorizationUrl generateAuthorizationUrl(String state, String redirectUri) {
        String url = "https://fake-auth.sellerintelligence.local/oauth/authorize?state=" + state + "&redirect_uri=" + redirectUri;
        return new OAuthAuthorizationUrl(url, state, OffsetDateTime.now().plusMinutes(15));
    }

    @Override
    public OAuthTokenResult exchangeAuthorizationCode(String code, String redirectUri) {
        return new OAuthTokenResult(
                "fake-access-token-" + code,
                "fake-refresh-token-" + code,
                "fake-ext-account-123",
                "test_scope",
                OffsetDateTime.now().plusHours(6),
                OffsetDateTime.now().plusMonths(6),
                "TEST"
        );
    }

    @Override
    public OAuthTokenResult refreshAccessToken(String refreshToken) {
        return new OAuthTokenResult(
                "fake-refreshed-access-token",
                "fake-refreshed-refresh-token",
                "fake-ext-account-123",
                "test_scope",
                OffsetDateTime.now().plusHours(6),
                OffsetDateTime.now().plusMonths(6),
                "TEST"
        );
    }

    @Override
    public void revokeToken(String accessToken, String externalAccountId) {
        // no-op in fake
    }

    @Override
    public boolean validateConnection(String accessToken, String externalAccountId) {
        return accessToken != null && !accessToken.isBlank();
    }
}
