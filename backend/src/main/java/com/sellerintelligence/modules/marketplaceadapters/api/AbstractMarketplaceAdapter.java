package com.sellerintelligence.modules.marketplaceadapters.api;

import com.sellerintelligence.modules.marketplaceadapters.model.CapabilityRegistry;
import com.sellerintelligence.modules.marketplaceadapters.model.ExecutionContext;
import com.sellerintelligence.modules.marketplaceadapters.model.MarketplaceOperation;
import com.sellerintelligence.modules.marketplaceadapters.resilience.ResiliencePipeline;

import java.util.Objects;

/**
 * Classe base para implementações de MarketplaceAdapter.
 * Encapsula a esteira de resiliência e o registro de capacidades.
 */
public abstract class AbstractMarketplaceAdapter implements MarketplaceAdapter {

    private final String marketplaceId;
    private final CapabilityRegistry capabilityRegistry;
    private final ResiliencePipeline resiliencePipeline;

    protected AbstractMarketplaceAdapter(
            String marketplaceId,
            CapabilityRegistry capabilityRegistry,
            ResiliencePipeline resiliencePipeline
    ) {
        this.marketplaceId = Objects.requireNonNull(marketplaceId, "marketplaceId é obrigatório");
        this.capabilityRegistry = Objects.requireNonNull(capabilityRegistry, "capabilityRegistry é obrigatório");
        this.resiliencePipeline = Objects.requireNonNull(resiliencePipeline, "resiliencePipeline é obrigatório");
    }

    @Override
    public String getMarketplaceId() {
        return marketplaceId;
    }

    @Override
    public CapabilityRegistry getCapabilityRegistry() {
        return capabilityRegistry;
    }

    @Override
    public <T, R> R execute(MarketplaceOperation<T, R> operation, T input, ExecutionContext context) throws Exception {
        return resiliencePipeline.execute(operation, input, context);
    }

    @Override
    public boolean isHealthy() {
        return true;
    }
}
