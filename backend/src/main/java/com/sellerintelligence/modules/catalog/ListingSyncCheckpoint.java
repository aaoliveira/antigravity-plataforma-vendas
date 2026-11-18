package com.sellerintelligence.modules.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Registra o cursor e o estado de sincronização paginada de anúncios por conta e marketplace.
 * Permite que falhas parciais (ex: rate limits ou timeout em página intermediária) sejam retomáveis
 * exatamente a partir do último checkpoint sem duplicar registros ou recomeçar do zero.
 */
@Entity
@Table(name = "listing_sync_checkpoints")
public class ListingSyncCheckpoint {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "seller_account_id", nullable = false)
    private UUID sellerAccountId;

    @Column(name = "marketplace", nullable = false, length = 50)
    private String marketplace;

    @Column(name = "sync_status", nullable = false, length = 50)
    private String syncStatus;

    @Column(name = "last_offset", nullable = false)
    private Integer lastOffset;

    @Column(name = "page_size", nullable = false)
    private Integer pageSize;

    @Column(name = "total_items_synced", nullable = false)
    private Integer totalItemsSynced;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "last_checkpoint_at", nullable = false)
    private Instant lastCheckpointAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    protected ListingSyncCheckpoint() {
        // JPA requirement
    }

    public ListingSyncCheckpoint(
            UUID id,
            UUID tenantId,
            UUID sellerAccountId,
            String marketplace,
            Integer pageSize
    ) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        this.sellerAccountId = Objects.requireNonNull(sellerAccountId, "sellerAccountId não pode ser nulo");
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace não pode ser nulo");
        this.pageSize = pageSize != null && pageSize > 0 ? pageSize : 50;
        this.lastOffset = 0;
        this.totalItemsSynced = 0;
        this.syncStatus = "IN_PROGRESS";
        this.startedAt = Instant.now();
        this.lastCheckpointAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getSellerAccountId() {
        return sellerAccountId;
    }

    public String getMarketplace() {
        return marketplace;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = Objects.requireNonNull(syncStatus, "syncStatus não pode ser nulo");
        this.lastCheckpointAt = Instant.now();
    }

    public Integer getLastOffset() {
        return lastOffset;
    }

    public void setLastOffset(Integer lastOffset) {
        this.lastOffset = Objects.requireNonNull(lastOffset, "lastOffset não pode ser nulo");
        this.lastCheckpointAt = Instant.now();
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getTotalItemsSynced() {
        return totalItemsSynced;
    }

    public void setTotalItemsSynced(Integer totalItemsSynced) {
        this.totalItemsSynced = totalItemsSynced;
        this.lastCheckpointAt = Instant.now();
    }

    public void incrementItemsSynced(int count) {
        this.totalItemsSynced += count;
        this.lastCheckpointAt = Instant.now();
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getLastCheckpointAt() {
        return lastCheckpointAt;
    }

    public void setLastCheckpointAt(Instant lastCheckpointAt) {
        this.lastCheckpointAt = lastCheckpointAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.lastCheckpointAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ListingSyncCheckpoint that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
