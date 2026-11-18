package com.sellerintelligence.modules.marketplaceadapters.exception;

/**
 * Exceção base para falhas do módulo de Marketplace Adapters.
 */
public class MarketplaceAdapterException extends RuntimeException {

    private final String marketplaceId;

    public MarketplaceAdapterException(String marketplaceId, String message) {
        super(String.format("[%s] %s", marketplaceId, message));
        this.marketplaceId = marketplaceId;
    }

    public MarketplaceAdapterException(String marketplaceId, String message, Throwable cause) {
        super(String.format("[%s] %s", marketplaceId, message), cause);
        this.marketplaceId = marketplaceId;
    }

    public String getMarketplaceId() {
        return marketplaceId;
    }
}
