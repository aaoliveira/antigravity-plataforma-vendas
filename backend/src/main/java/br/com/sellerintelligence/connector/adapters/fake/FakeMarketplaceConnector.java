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

    @Override
    public ConnectorSyncBatch<ConnectorListing> fetchListings(String accessToken, String externalAccountId, String cursor, int limit) {
        if ("END".equals(cursor)) {
            return new ConnectorSyncBatch<>(List.of(), null, false);
        }
        List<ConnectorListing> sampleListings = List.of(
                new ConnectorListing(
                        "FAKE-ITEM-001",
                        "SKU-TEST-001",
                        "Produto Teste 1 Fake Marketplace",
                        "ACTIVE",
                        new java.math.BigDecimal("99.90"),
                        "BRL",
                        50,
                        10,
                        "gold_special",
                        "https://fake.local/item/1",
                        "https://fake.local/thumb/1.jpg",
                        "{\"raw\": true}",
                        java.time.Instant.now()
                ),
                new ConnectorListing(
                        "FAKE-ITEM-002",
                        "SKU-TEST-002",
                        "Produto Teste 2 Fake Marketplace",
                        "PAUSED",
                        new java.math.BigDecimal("149.00"),
                        "BRL",
                        15,
                        2,
                        "gold_pro",
                        "https://fake.local/item/2",
                        "https://fake.local/thumb/2.jpg",
                        "{\"raw\": true}",
                        java.time.Instant.now()
                )
        );
        return new ConnectorSyncBatch<>(sampleListings, "END", false);
    }

    @Override
    public ConnectorSyncBatch<ConnectorOrder> fetchOrders(String accessToken, String externalAccountId, java.time.Instant from, String cursor, int limit) {
        if ("END".equals(cursor)) {
            return new ConnectorSyncBatch<>(List.of(), null, false);
        }
        ConnectorOrderItem item1 = new ConnectorOrderItem(
                "FAKE-ITEM-001",
                "SKU-TEST-001",
                "Produto Teste 1 Fake Marketplace",
                new java.math.BigDecimal("99.90"),
                1,
                new java.math.BigDecimal("12.98")
        );
        ConnectorOrder order1 = new ConnectorOrder(
                "FAKE-ORDER-99901",
                "PAID",
                new java.math.BigDecimal("99.90"),
                "BRL",
                java.time.Instant.now().minusSeconds(3600),
                "comprador_teste_1",
                "SHIP-88801",
                new java.math.BigDecimal("15.50"),
                new java.math.BigDecimal("12.98"),
                List.of(item1),
                "{\"order_raw\": true}",
                java.time.Instant.now()
        );
        return new ConnectorSyncBatch<>(List.of(order1), "END", false);
    }
}
