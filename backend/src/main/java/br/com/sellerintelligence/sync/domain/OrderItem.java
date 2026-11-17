package br.com.sellerintelligence.sync.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "external_item_id", length = 150)
    private String externalItemId;

    @Column(name = "sku", length = 120)
    private String sku;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "sale_fee", precision = 15, scale = 2)
    private BigDecimal saleFee;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OrderItem() {}

    public OrderItem(UUID tenantId, Order order, String externalItemId, String sku,
                     String title, BigDecimal unitPrice, Integer quantity, BigDecimal saleFee) {
        this.id = UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.order = Objects.requireNonNull(order, "order cannot be null");
        this.externalItemId = externalItemId;
        this.sku = sku != null && !sku.isBlank() ? sku.trim() : null;
        this.title = Objects.requireNonNull(title, "title cannot be null").trim();
        this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice cannot be null");
        this.quantity = Objects.requireNonNull(quantity, "quantity cannot be null");
        this.saleFee = saleFee;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
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

    public BigDecimal getSaleFee() {
        return saleFee;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
