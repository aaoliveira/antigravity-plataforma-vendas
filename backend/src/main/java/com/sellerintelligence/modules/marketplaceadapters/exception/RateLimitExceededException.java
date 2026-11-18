package com.sellerintelligence.modules.marketplaceadapters.exception;

import java.time.Duration;

/**
 * Lançada quando a cota ou limite de taxa do conector ou marketplace é excedida.
 */
public class RateLimitExceededException extends MarketplaceAdapterException {

    private final Duration retryAfter;

    public RateLimitExceededException(String marketplaceId, String message, Duration retryAfter) {
        super(marketplaceId, message);
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
