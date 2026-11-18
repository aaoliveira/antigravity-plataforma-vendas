package com.sellerintelligence.modules.orderingestion.model;

import java.math.BigDecimal;

/**
 * Item individual contido no payload de um pedido retornado pela API oficial do canal.
 */
public record RawOrderItem(
        String externalItemId,
        String externalListingId,
        String sku,
        String title,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal totalPrice
) {}
