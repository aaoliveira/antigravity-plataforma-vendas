package com.sellerintelligence.modules.orderingestion.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Representação normalizada e minimizada do pedido próprio retornado pela API oficial do canal.
 */
public record RawOrder(
        String externalOrderId,
        UUID sellerAccountId,
        String marketplace,
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
        Instant orderCreatedAt,
        String rawPayloadJson,
        String schemaVersion,
        String origin,
        List<RawOrderItem> items
) {}
