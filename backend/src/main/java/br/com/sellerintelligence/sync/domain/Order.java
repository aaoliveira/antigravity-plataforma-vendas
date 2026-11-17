package br.com.sellerintelligence.sync.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "marketplace", nullable = false, length = 50)
    private Marketplace marketplace;

    @Column(name = "external_order_id", nullable = false, length = 150)
    private String externalOrderId;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "order_date", nullable = false)
    private Instant orderDate;

    @Column(name = "buyer_nickname", length = 255)
    private String buyerNickname;

    @Column(name = "shipping_id", length = 150)
    private String shippingId;

    @Column(name = "shipping_cost_seller", precision = 15, scale = 2)
    private BigDecimal shippingCostSeller;

    @Column(name = "marketplace_fee", precision = 15, scale = 2)
    private BigDecimal marketplaceFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "provenance", nullable = false, length = 50)
    private DataProvenance provenance;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "origin_updated_at")
    private Instant originUpdatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {}

    public Order(UUID tenantId, UUID storeId, Marketplace marketplace, String externalOrderId,
                 String status, BigDecimal totalAmount, String currency, Instant orderDate,
                 String buyerNickname, String shippingId, BigDecimal shippingCostSeller,
                 BigDecimal marketplaceFee, DataProvenance provenance, String rawPayload,
                 Instant originUpdatedAt) {
        this.id = UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.storeId = Objects.requireNonNull(storeId, "storeId cannot be null");
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace cannot be null");
        this.externalOrderId = Objects.requireNonNull(externalOrderId, "externalOrderId cannot be null").trim();
        this.status = Objects.requireNonNull(status, "status cannot be null").toUpperCase();
        this.totalAmount = Objects.requireNonNull(totalAmount, "totalAmount cannot be null");
        this.currency = currency != null && !currency.isBlank() ? currency.toUpperCase() : "BRL";
        this.orderDate = Objects.requireNonNull(orderDate, "orderDate cannot be null");
        this.buyerNickname = buyerNickname;
        this.shippingId = shippingId;
        this.shippingCostSeller = shippingCostSeller;
        this.marketplaceFee = marketplaceFee;
        this.provenance = provenance != null ? provenance : DataProvenance.IMPORTED;
        this.rawPayload = rawPayload;
        this.originUpdatedAt = originUpdatedAt;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void update(String status, BigDecimal totalAmount, BigDecimal shippingCostSeller,
                       BigDecimal marketplaceFee, String rawPayload, Instant originUpdatedAt) {
        if (status != null && !status.isBlank()) {
            this.status = status.toUpperCase();
        }
        if (totalAmount != null) {
            this.totalAmount = totalAmount;
        }
        this.shippingCostSeller = shippingCostSeller;
        this.marketplaceFee = marketplaceFee;
        if (rawPayload != null) {
            this.rawPayload = rawPayload;
        }
        this.originUpdatedAt = originUpdatedAt;
        this.updatedAt = Instant.now();
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void clearItems() {
        items.clear();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getStoreId() {
        return storeId;
    }

    public Marketplace getMarketplace() {
        return marketplace;
    }

    public String getExternalOrderId() {
        return externalOrderId;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getOrderDate() {
        return orderDate;
    }

    public String getBuyerNickname() {
        return buyerNickname;
    }

    public String getShippingId() {
        return shippingId;
    }

    public BigDecimal getShippingCostSeller() {
        return shippingCostSeller;
    }

    public BigDecimal getMarketplaceFee() {
        return marketplaceFee;
    }

    public DataProvenance getProvenance() {
        return provenance;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public Instant getOriginUpdatedAt() {
        return originUpdatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}
