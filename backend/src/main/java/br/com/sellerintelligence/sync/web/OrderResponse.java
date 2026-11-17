package br.com.sellerintelligence.sync.web;

import br.com.sellerintelligence.sync.domain.DataProvenance;
import br.com.sellerintelligence.sync.domain.Order;
import br.com.sellerintelligence.tenancy.store.Marketplace;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID storeId,
        Marketplace marketplace,
        String externalOrderId,
        String status,
        BigDecimal totalAmount,
        String currency,
        Instant orderDate,
        String buyerNickname,
        String shippingId,
        BigDecimal shippingCostSeller,
        BigDecimal marketplaceFee,
        DataProvenance provenance,
        Instant originUpdatedAt,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(Order o) {
        List<OrderItemResponse> items = o.getItems() != null
                ? o.getItems().stream().map(OrderItemResponse::from).toList()
                : List.of();

        return new OrderResponse(
                o.getId(),
                o.getStoreId(),
                o.getMarketplace(),
                o.getExternalOrderId(),
                o.getStatus(),
                o.getTotalAmount(),
                o.getCurrency(),
                o.getOrderDate(),
                o.getBuyerNickname(),
                o.getShippingId(),
                o.getShippingCostSeller(),
                o.getMarketplaceFee(),
                o.getProvenance(),
                o.getOriginUpdatedAt(),
                items
        );
    }
}
