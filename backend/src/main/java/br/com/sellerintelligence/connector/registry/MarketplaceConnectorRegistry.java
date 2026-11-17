package br.com.sellerintelligence.connector.registry;

import br.com.sellerintelligence.connector.model.ConnectorCapability;
import br.com.sellerintelligence.connector.ports.MarketplaceConnector;
import br.com.sellerintelligence.infrastructure.error.BusinessException;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MarketplaceConnectorRegistry {

    private final Map<Marketplace, MarketplaceConnector> connectors = new EnumMap<>(Marketplace.class);

    public MarketplaceConnectorRegistry(List<MarketplaceConnector> connectorList) {
        for (MarketplaceConnector connector : connectorList) {
            connectors.put(connector.getMarketplace(), connector);
        }
    }

    public MarketplaceConnector getConnector(Marketplace marketplace) {
        MarketplaceConnector connector = connectors.get(marketplace);
        if (connector == null) {
            throw new BusinessException(
                    "UNSUPPORTED_MARKETPLACE",
                    "No active connector implementation found for marketplace: " + marketplace,
                    HttpStatus.BAD_REQUEST
            );
        }
        return connector;
    }

    public boolean hasConnector(Marketplace marketplace) {
        return connectors.containsKey(marketplace);
    }

    public Map<Marketplace, List<ConnectorCapability>> getAllCapabilities() {
        Map<Marketplace, List<ConnectorCapability>> result = new EnumMap<>(Marketplace.class);
        for (Map.Entry<Marketplace, MarketplaceConnector> entry : connectors.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getCapabilities());
        }
        return Collections.unmodifiableMap(result);
    }
}
