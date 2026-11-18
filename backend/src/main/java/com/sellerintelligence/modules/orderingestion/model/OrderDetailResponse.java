package com.sellerintelligence.modules.orderingestion.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Resposta detalhada de pedido para o frontend e integrações.
 */
public record OrderDetailResponse(
        UUID id,
        UUID tenantId,
        UUID sellerAccountId,
        String marketplace,
        String externalOrderId,
        OrderStatus status,
        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal totalAmount,
        String currency,
        String buyerName,
        String buyerDocument,
        String trackingNumber,
        String cancellationReason,
        Instant cancelledAt,
        Instant returnedAt,
        String rawPayloadJson,
        String schemaVersion,
        String origin,
        Instant orderCreatedAt,
        Instant syncedAt,
        List<OrderItemResponse> items
) {
    public OrderDetailResponse {
        if (items == null) {
            items = List.of();
        }
    }
}
