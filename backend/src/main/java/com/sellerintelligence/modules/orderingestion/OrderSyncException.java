package com.sellerintelligence.modules.orderingestion;

/**
 * Exceção lançada em caso de falha na sincronização de pedidos com marketplaces.
 */
public class OrderSyncException extends RuntimeException {

    public OrderSyncException(String message) {
        super(message);
    }

    public OrderSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
