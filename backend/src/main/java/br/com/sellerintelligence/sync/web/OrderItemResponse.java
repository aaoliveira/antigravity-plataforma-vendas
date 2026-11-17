package br.com.sellerintelligence.sync.web;

import br.com.sellerintelligence.sync.domain.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        String externalItemId,
        String sku,
        String title,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal saleFee
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getExternalItemId(),
                item.getSku(),
                item.getTitle(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSaleFee()
        );
    }
}
