package com.sellerintelligence.modules.orderingestion.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Representação de item de pedido para a camada de apresentação/API.
 */
public record OrderItemResponse(
        UUID id,
        String externalItemId,
        String externalListingId,
        String sku,
        String title,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal totalItemPrice
) {}
