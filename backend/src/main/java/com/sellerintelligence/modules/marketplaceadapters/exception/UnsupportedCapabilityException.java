package com.sellerintelligence.modules.marketplaceadapters.exception;

import com.sellerintelligence.modules.marketplaceadapters.model.MarketplaceCapability;

/**
 * Lançada quando se tenta invocar uma capacidade não suportada oficialmente pelo marketplace
 * (ex: estimativa de faturamento de concorrentes sem API pública).
 */
public class UnsupportedCapabilityException extends MarketplaceAdapterException {

    private final MarketplaceCapability capability;

    public UnsupportedCapabilityException(String marketplaceId, MarketplaceCapability capability) {
        super(marketplaceId, String.format(
                "A capacidade '%s' é UNSUPPORTED no canal '%s'. Proibido obter esse dado por scraping hostil ou engenharia reversa.",
                capability, marketplaceId
        ));
        this.capability = capability;
    }

    public MarketplaceCapability getCapability() {
        return capability;
    }
}
