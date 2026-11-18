package com.sellerintelligence.modules.marketplaceadapters.exception;

/**
 * Lançada quando uma chamada é rejeitada imediatamente porque o Circuit Breaker está no estado OPEN.
 */
public class CircuitBreakerOpenException extends MarketplaceAdapterException {

    public CircuitBreakerOpenException(String marketplaceId) {
        super(marketplaceId, String.format(
                "O Circuit Breaker do canal '%s' está ABERTO devido a falhas consecutivas anteriores. Chamada rejeitada preventivamente (Fail-Closed).",
                marketplaceId
        ));
    }
}
