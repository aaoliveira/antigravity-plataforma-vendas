package com.sellerintelligence.modules.productmonitoring.entity;

import com.sellerintelligence.modules.productmonitoring.model.MonitoredItemStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade JPA para item/anúncio monitorado em um grupo.
 */
@Entity
@Table(name = "monitored_items")
public class MonitoredItemRecord {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "marketplace", nullable = false, length = 50)
    private String marketplace;

    @Column(name = "source_policy_id", nullable = false, length = 100)
    private String sourcePolicyId;

    @Column(name = "external_listing_id", nullable = false, length = 100)
    private String externalListingId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "initial_price", precision = 15, scale = 4)
    private BigDecimal initialPrice;

    @Column(name = "current_price", precision = 15, scale = 4)
    private BigDecimal currentPrice;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "seller_name", length = 150)
    private String sellerName;

    @Column(name = "item_condition", length = 30)
    private String itemCondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MonitoredItemStatus status;

    @Column(name = "monitoring_interval_minutes", nullable = false)
    private int monitoringIntervalMinutes;

    @Column(name = "last_snapshot_at")
    private Instant lastSnapshotAt;

    @Column(name = "next_run_at", nullable = false)
    private Instant nextRunAt;

    @Column(name = "first_snapshot_at")
    private Instant firstSnapshotAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MonitoredItemRecord() {
    }

    public MonitoredItemRecord(
            UUID id,
            UUID tenantId,
            UUID groupId,
            String marketplace,
            String sourcePolicyId,
            String externalListingId,
            String title,
            BigDecimal initialPrice,
            BigDecimal currentPrice,
            String currency,
            String sourceUrl,
            String sellerName,
            String itemCondition,
            MonitoredItemStatus status,
            int monitoringIntervalMinutes,
            Instant lastSnapshotAt,
            Instant nextRunAt,
            Instant firstSnapshotAt,
            Instant deletedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.groupId = Objects.requireNonNull(groupId, "groupId cannot be null");
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace cannot be null");
        this.sourcePolicyId = Objects.requireNonNull(sourcePolicyId, "sourcePolicyId cannot be null");
        this.externalListingId = Objects.requireNonNull(externalListingId, "externalListingId cannot be null");
        this.title = Objects.requireNonNull(title, "title cannot be null");
        this.initialPrice = initialPrice;
        this.currentPrice = currentPrice;
        this.currency = currency != null ? currency : "BRL";
        this.sourceUrl = sourceUrl;
        this.sellerName = sellerName;
        this.itemCondition = itemCondition != null ? itemCondition : "NEW";
        this.status = status != null ? status : MonitoredItemStatus.ACTIVE;
        this.monitoringIntervalMinutes = Math.max(monitoringIntervalMinutes, 15);
        this.lastSnapshotAt = lastSnapshotAt;
        this.nextRunAt = nextRunAt != null ? nextRunAt : Instant.now();
        this.firstSnapshotAt = firstSnapshotAt;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
        this.updatedAt = Instant.now();
    }

    public String getMarketplace() {
        return marketplace;
    }

    public String getSourcePolicyId() {
        return sourcePolicyId;
    }

    public String getExternalListingId() {
        return externalListingId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = Instant.now();
    }

    public BigDecimal getInitialPrice() {
        return initialPrice;
    }

    public void setInitialPrice(BigDecimal initialPrice) {
        this.initialPrice = initialPrice;
        this.updatedAt = Instant.now();
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
        this.updatedAt = Instant.now();
    }

    public String getCurrency() {
        return currency;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
        this.updatedAt = Instant.now();
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
        this.updatedAt = Instant.now();
    }

    public String getItemCondition() {
        return itemCondition;
    }

    public void setItemCondition(String itemCondition) {
        this.itemCondition = itemCondition;
        this.updatedAt = Instant.now();
    }

    public MonitoredItemStatus getStatus() {
        return status;
    }

    public void setStatus(MonitoredItemStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public int getMonitoringIntervalMinutes() {
        return monitoringIntervalMinutes;
    }

    public void setMonitoringIntervalMinutes(int monitoringIntervalMinutes) {
        this.monitoringIntervalMinutes = Math.max(monitoringIntervalMinutes, 15);
        this.updatedAt = Instant.now();
    }

    public Instant getLastSnapshotAt() {
        return lastSnapshotAt;
    }

    public void setLastSnapshotAt(Instant lastSnapshotAt) {
        this.lastSnapshotAt = lastSnapshotAt;
        this.updatedAt = Instant.now();
    }

    public Instant getNextRunAt() {
        return nextRunAt;
    }

    public void setNextRunAt(Instant nextRunAt) {
        this.nextRunAt = nextRunAt;
        this.updatedAt = Instant.now();
    }

    public Instant getFirstSnapshotAt() {
        return firstSnapshotAt;
    }

    public void setFirstSnapshotAt(Instant firstSnapshotAt) {
        this.firstSnapshotAt = firstSnapshotAt;
        this.updatedAt = Instant.now();
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
