package com.sellerintelligence.modules.orderingestion.model;

import java.util.UUID;

/**
 * Parâmetros de requisição para busca paginada de pedidos com cursor ou offset.
 */
public record OrderPageRequest(
        UUID sellerAccountId,
        int pageSize,
        String cursorToken,
        int offset
) {
    public OrderPageRequest {
        if (pageSize <= 0) {
            pageSize = 50;
        }
        if (offset < 0) {
            offset = 0;
        }
    }
}
