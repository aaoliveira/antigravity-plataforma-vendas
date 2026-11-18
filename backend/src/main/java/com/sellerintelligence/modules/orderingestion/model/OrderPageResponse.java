package com.sellerintelligence.modules.orderingestion.model;

import java.util.List;

/**
 * Resposta paginada de pedidos retornada pelo adapter oficial.
 */
public record OrderPageResponse(
        List<RawOrder> orders,
        String nextCursorToken,
        boolean hasMore,
        int totalOrders
) {
    public OrderPageResponse {
        if (orders == null) {
            orders = List.of();
        }
    }
}
