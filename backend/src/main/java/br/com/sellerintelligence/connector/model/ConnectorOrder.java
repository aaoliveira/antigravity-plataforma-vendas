package br.com.sellerintelligence.connector.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ConnectorOrder(
        String externalOrderId,
        String status,
        BigDecimal totalAmount,
        String currency,
        Instant orderDate,
        String buyerNickname,
        String shippingId,
        BigDecimal shippingCostSeller,
        BigDecimal marketplaceFee,
        List<ConnectorOrderItem> items,
        String rawPayload,
        Instant originUpdatedAt
) {}
