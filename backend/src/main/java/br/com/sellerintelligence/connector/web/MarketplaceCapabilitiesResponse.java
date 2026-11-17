package br.com.sellerintelligence.connector.web;

import br.com.sellerintelligence.connector.model.ConnectorCapability;
import br.com.sellerintelligence.tenancy.store.Marketplace;

import java.util.List;

public record MarketplaceCapabilitiesResponse(
        Marketplace marketplace,
        List<ConnectorCapability> capabilities
) {
}
