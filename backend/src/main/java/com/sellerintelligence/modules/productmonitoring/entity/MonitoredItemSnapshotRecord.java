package com.sellerintelligence.modules.productmonitoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade JPA para snapshot histórico de preço e disponibilidade de item monitorado.
 * O histórico inicia estritamente a partir da data de inclusão do item.
 */
@Entity
@Table(name = "monitored_item_snapshots")
public class MonitoredItemSnapshotRecord {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "item_id", nullable = false, updatable = false)
    private UUID itemId;

    @Column(name = "group_id", nullable = false, updatable = false)
    private UUID groupId;

    @Column(name = "marketplace", nullable = false, length = 50)
    private String marketplace;

    @Column(name = "external_listing_id", nullable = false, length = 100)
    private String externalListingId;

    @Column(name = "price", nullable = false, precision = 15, scale = 4)
    private BigDecimal price;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "shipping_cost", precision = 15, scale = 4)
    private BigDecimal shippingCost;

    @Column(name = "shipping_status", nullable = false, length = 30)
    private String shippingStatus;

    @Column(name = "available_quantity")
    private Integer availableQuantity;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "provenance_type", nullable = false, length = 50)
    private String provenanceType;

    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "is_effective_snapshot", nullable = false)
    private boolean isEffectiveSnapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MonitoredItemSnapshotRecord() {
    }

    public MonitoredItemSnapshotRecord(
            UUID id,
            UUID tenantId,
            UUID itemId,
            UUID groupId,
            String marketplace,
            String externalListingId,
            BigDecimal price,
            String currency,
            BigDecimal shippingCost,
            String shippingStatus,
            Integer availableQuantity,
            String status,
            String provenanceType,
            Instant collectedAt,
            Instant observedAt,
            boolean isEffectiveSnapshot,
            Instant createdAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.itemId = Objects.requireNonNull(itemId, "itemId cannot be null");
        this.groupId = Objects.requireNonNull(groupId, "groupId cannot be null");
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace cannot be null");
        this.externalListingId = Objects.requireNonNull(externalListingId, "externalListingId cannot be null");
        this.price = Objects.requireNonNull(price, "price cannot be null");
        this.currency = currency != null ? currency : "BRL";
        this.shippingCost = shippingCost;
        this.shippingStatus = shippingStatus != null ? shippingStatus : (shippingCost != null ? "KNOWN" : "UNKNOWN");
        this.availableQuantity = availableQuantity;
        this.status = status != null ? status : "ACTIVE";
        this.provenanceType = provenanceType != null ? provenanceType : "OBSERVED";
        this.collectedAt = collectedAt != null ? collectedAt : Instant.now();
        this.observedAt = observedAt != null ? observedAt : Instant.now();
        this.isEffectiveSnapshot = isEffectiveSnapshot;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getItemId() {
        return itemId;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public String getMarketplace() {
        return marketplace;
    }

    public String getExternalListingId() {
        return externalListingId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getShippingCost() {
        return shippingCost;
    }

    public String getShippingStatus() {
        return shippingStatus;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public String getStatus() {
        return status;
    }

    public String getProvenanceType() {
        return provenanceType;
    }

    public Instant getCollectedAt() {
        return collectedAt;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public boolean isEffectiveSnapshot() {
        return isEffectiveSnapshot;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
