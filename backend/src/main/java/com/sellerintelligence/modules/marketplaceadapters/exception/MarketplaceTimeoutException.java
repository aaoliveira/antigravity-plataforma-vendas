package com.sellerintelligence.modules.marketplaceadapters.exception;

import java.time.Duration;

/**
 * Lançada quando a operação no marketplace excede o tempo limite configurado.
 */
public class MarketplaceTimeoutException extends MarketplaceAdapterException {

    public MarketplaceTimeoutException(String marketplaceId, Duration timeout) {
        super(marketplaceId, String.format(
                "A operação no canal '%s' excedeu o tempo limite configurado de %d ms.",
                marketplaceId, timeout.toMillis()
        ));
    }
}
