package com.sellerintelligence.modules.orderingestion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Item pertencente a um pedido próprio de venda.
 * Escopado por tenant_id e protegido por RLS.
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "product_variant_id")
    private UUID productVariantId;

    @Column(name = "external_listing_id", length = 100)
    private String externalListingId;

    @Column(name = "external_item_id", length = 100)
    private String externalItemId;

    @Column(name = "sku", length = 100)
    private String sku;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "total_item_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal totalItemPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrderItem() {
        // JPA requirement
    }

    public OrderItem(
            UUID id,
            UUID tenantId,
            UUID orderId,
            UUID productId,
            UUID productVariantId,
            String externalListingId,
            String externalItemId,
            String sku,
            String title,
            BigDecimal unitPrice,
            Integer quantity,
            BigDecimal totalItemPrice
    ) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        this.orderId = Objects.requireNonNull(orderId, "orderId não pode ser nulo");
        this.productId = productId;
        this.productVariantId = productVariantId;
        this.externalListingId = externalListingId;
        this.externalItemId = externalItemId;
        this.sku = sku;
        this.title = Objects.requireNonNull(title, "title não pode ser nulo");
        this.unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        this.quantity = quantity != null ? quantity : 1;
        this.totalItemPrice = totalItemPrice != null ? totalItemPrice : BigDecimal.ZERO;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateDetails(BigDecimal newUnitPrice, Integer newQuantity, BigDecimal newTotalPrice) {
        if (newUnitPrice != null) this.unitPrice = newUnitPrice;
        if (newQuantity != null) this.quantity = newQuantity;
        if (newTotalPrice != null) this.totalItemPrice = newTotalPrice;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getProductId() {
        return productId;
    }

    public UUID getProductVariantId() {
        return productVariantId;
    }

    public String getExternalListingId() {
        return externalListingId;
    }

    public String getExternalItemId() {
        return externalItemId;
    }

    public String getSku() {
        return sku;
    }

    public String getTitle() {
        return title;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getTotalItemPrice() {
        return totalItemPrice;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
