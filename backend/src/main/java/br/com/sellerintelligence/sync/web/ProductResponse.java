package br.com.sellerintelligence.sync.web;

import br.com.sellerintelligence.sync.domain.DataProvenance;
import br.com.sellerintelligence.sync.domain.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID storeId,
        String sku,
        String title,
        String brand,
        String category,
        String barcode,
        BigDecimal costPrice,
        DataProvenance provenance,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getStoreId(),
                p.getSku(),
                p.getTitle(),
                p.getBrand(),
                p.getCategory(),
                p.getBarcode(),
                p.getCostPrice(),
                p.getProvenance(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
