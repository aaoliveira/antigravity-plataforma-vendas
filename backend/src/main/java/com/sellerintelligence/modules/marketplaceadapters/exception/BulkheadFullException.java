package com.sellerintelligence.modules.marketplaceadapters.exception;

/**
 * Lançada quando o limite de chamadas concorrentes para o conector é atingido.
 */
public class BulkheadFullException extends MarketplaceAdapterException {

    public BulkheadFullException(String marketplaceId, int maxConcurrency) {
        super(marketplaceId, String.format(
                "Capacidade máxima do Bulkhead atingida para o canal '%s' (%d chamadas simultâneas). Chamada rejeitada para evitar saturação.",
                marketplaceId, maxConcurrency
        ));
    }
}
