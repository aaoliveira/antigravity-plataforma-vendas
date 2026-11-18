package com.sellerintelligence.modules.orderingestion.model;

/**
 * Estados do ciclo de vida de um pedido de venda próprio no marketplace.
 */
public enum OrderStatus {
    PENDING,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    RETURNED
}
