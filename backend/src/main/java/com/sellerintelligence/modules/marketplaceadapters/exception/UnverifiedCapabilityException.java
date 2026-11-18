package com.sellerintelligence.modules.marketplaceadapters.exception;

import com.sellerintelligence.modules.marketplaceadapters.model.MarketplaceCapability;

/**
 * Lançada quando se tenta invocar uma capacidade sem evidência documental comprovada ou não homologada.
 * Impõe o princípio Fail-Closed do PRD e de MARKETPLACE_POLICIES.md.
 */
public class UnverifiedCapabilityException extends MarketplaceAdapterException {

    private final MarketplaceCapability capability;

    public UnverifiedCapabilityException(String marketplaceId, MarketplaceCapability capability) {
        super(marketplaceId, String.format(
                "A capacidade '%s' está em estado UNVERIFIED ou sem evidência documental oficial registrada. Execução bloqueada por política de segurança.",
                capability
        ));
        this.capability = capability;
    }

    public MarketplaceCapability getCapability() {
        return capability;
    }
}
