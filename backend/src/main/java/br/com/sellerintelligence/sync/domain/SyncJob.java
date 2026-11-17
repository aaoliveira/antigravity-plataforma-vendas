package br.com.sellerintelligence.sync.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sync_jobs")
public class SyncJob {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "marketplace", nullable = false, length = 50)
    private Marketplace marketplace;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 50)
    private SyncResourceType resourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 50)
    private SyncTriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private SyncStatus status;

    @Column(name = "items_processed", nullable = false)
    private int itemsProcessed;

    @Column(name = "items_succeeded", nullable = false)
    private int itemsSucceeded;

    @Column(name = "items_failed", nullable = false)
    private int itemsFailed;

    @Column(name = "cursor", columnDefinition = "TEXT")
    private String cursor;

    @Column(name = "error_summary", columnDefinition = "TEXT")
    private String errorSummary;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SyncJob() {}

    public SyncJob(UUID tenantId, UUID storeId, Marketplace marketplace,
                   SyncResourceType resourceType, SyncTriggerType triggerType) {
        this.id = UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.storeId = Objects.requireNonNull(storeId, "storeId cannot be null");
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace cannot be null");
        this.resourceType = Objects.requireNonNull(resourceType, "resourceType cannot be null");
        this.triggerType = Objects.requireNonNull(triggerType, "triggerType cannot be null");
        this.status = SyncStatus.PENDING;
        this.itemsProcessed = 0;
        this.itemsSucceeded = 0;
        this.itemsFailed = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void start() {
        this.status = SyncStatus.RUNNING;
        this.startedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void recordSuccess(int count) {
        this.itemsProcessed += count;
        this.itemsSucceeded += count;
        this.updatedAt = Instant.now();
    }

    public void recordFailure(int count, String error) {
        this.itemsProcessed += count;
        this.itemsFailed += count;
        this.errorSummary = error;
        this.updatedAt = Instant.now();
    }

    public void complete(String cursor) {
        this.status = SyncStatus.COMPLETED;
        this.cursor = cursor;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void fail(String errorSummary) {
        this.status = SyncStatus.FAILED;
        this.errorSummary = errorSummary;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
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

    public SyncResourceType getResourceType() {
        return resourceType;
    }

    public SyncTriggerType getTriggerType() {
        return triggerType;
    }

    public SyncStatus getStatus() {
        return status;
    }

    public int getItemsProcessed() {
        return itemsProcessed;
    }

    public int getItemsSucceeded() {
        return itemsSucceeded;
    }

    public int getItemsFailed() {
        return itemsFailed;
    }

    public String getCursor() {
        return cursor;
    }

    public void setCursor(String cursor) {
        this.cursor = cursor;
    }

    public String getErrorSummary() {
        return errorSummary;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
