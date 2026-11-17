package br.com.sellerintelligence.connector.registry;

import br.com.sellerintelligence.connector.adapters.amazon.AmazonSpApiConnector;
import br.com.sellerintelligence.connector.adapters.mercadolivre.MercadoLivreConnector;
import br.com.sellerintelligence.connector.adapters.shopee.ShopeeConnector;
import br.com.sellerintelligence.connector.model.CapabilityStatus;
import br.com.sellerintelligence.connector.model.ConnectorCapabilityType;
import br.com.sellerintelligence.connector.ports.MarketplaceConnector;
import br.com.sellerintelligence.infrastructure.error.BusinessException;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketplaceConnectorRegistryTest {

    private MarketplaceConnectorRegistry registry;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        MarketplaceConnector ml = new MercadoLivreConnector("dummy-app", "dummy-sec", "https://auth.ml", "https://api.ml", mapper);
        MarketplaceConnector shopee = new ShopeeConnector("123456", "partner-key", "https://partner.shopee", mapper);
        MarketplaceConnector amazon = new AmazonSpApiConnector("dummy-amz-app", "dummy-amz-sec", "https://api.amazon.com/auth/o2/token", "https://sellercentral.amazon.com.br/apps/authorize/consent", "https://sellingpartnerapi-na.amazon.com", "A2Q3Y263D00KWC", mapper);
        registry = new MarketplaceConnectorRegistry(List.of(ml, shopee, amazon));
    }

    @Test
    @DisplayName("Registry provides connectors for Mercado Livre, Shopee and Amazon")
    void registryContainsKnownConnectors() {
        assertThat(registry.hasConnector(Marketplace.MERCADO_LIVRE)).isTrue();
        assertThat(registry.hasConnector(Marketplace.SHOPEE)).isTrue();
        assertThat(registry.hasConnector(Marketplace.AMAZON)).isTrue();

        MarketplaceConnector ml = registry.getConnector(Marketplace.MERCADO_LIVRE);
        assertThat(ml.getMarketplace()).isEqualTo(Marketplace.MERCADO_LIVRE);
        assertThat(ml.getCapabilityStatus(ConnectorCapabilityType.CATALOG)).isEqualTo(CapabilityStatus.SUPPORTED);
        assertThat(ml.getCapabilityStatus(ConnectorCapabilityType.ADS)).isEqualTo(CapabilityStatus.PENDING_VALIDATION);

        MarketplaceConnector shopee = registry.getConnector(Marketplace.SHOPEE);
        assertThat(shopee.getMarketplace()).isEqualTo(Marketplace.SHOPEE);
        assertThat(shopee.getCapabilityStatus(ConnectorCapabilityType.ADS)).isEqualTo(CapabilityStatus.UNSUPPORTED);

        MarketplaceConnector amazon = registry.getConnector(Marketplace.AMAZON);
        assertThat(amazon.getMarketplace()).isEqualTo(Marketplace.AMAZON);
        assertThat(amazon.getCapabilityStatus(ConnectorCapabilityType.CATALOG)).isEqualTo(CapabilityStatus.SUPPORTED);
        assertThat(amazon.getCapabilityStatus(ConnectorCapabilityType.ORDERS)).isEqualTo(CapabilityStatus.SUPPORTED);
        assertThat(amazon.getCapabilityStatus(ConnectorCapabilityType.ADS)).isEqualTo(CapabilityStatus.UNSUPPORTED);
        assertThat(amazon.getCapabilityStatus(ConnectorCapabilityType.PUBLIC_SEARCH)).isEqualTo(CapabilityStatus.UNSUPPORTED);
    }

    @Test
    @DisplayName("Unregistered marketplace throws BusinessException")
    void unsupportedMarketplaceThrowsException() {
        assertThatThrownBy(() -> registry.getConnector(Marketplace.OUTRO))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Registry exports all capabilities for UI and API consumers")
    void exportAllCapabilities() {
        var capabilities = registry.getAllCapabilities();
        assertThat(capabilities).containsKey(Marketplace.MERCADO_LIVRE);
        assertThat(capabilities).containsKey(Marketplace.SHOPEE);
        assertThat(capabilities).containsKey(Marketplace.AMAZON);
        assertThat(capabilities.get(Marketplace.MERCADO_LIVRE)).isNotEmpty();
        assertThat(capabilities.get(Marketplace.AMAZON)).isNotEmpty();
    }
}

