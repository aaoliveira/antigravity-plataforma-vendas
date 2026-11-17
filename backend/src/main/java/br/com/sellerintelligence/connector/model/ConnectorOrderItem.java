package br.com.sellerintelligence.connector.model;

import java.math.BigDecimal;

public record ConnectorOrderItem(
        String externalItemId,
        String sku,
        String title,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal saleFee
) {}
