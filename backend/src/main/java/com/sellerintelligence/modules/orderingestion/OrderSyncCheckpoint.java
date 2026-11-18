package com.sellerintelligence.modules.orderingestion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Registra o estado da sincronização paginada e cursor de pedidos por tenant, conta e canal.
 */
@Entity
@Table(name = "order_sync_checkpoints")
public class OrderSyncCheckpoint {

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

    @Column(name = "cursor_token", length = 255)
    private String cursorToken;

    @Column(name = "last_offset", nullable = false)
    private int lastOffset;

    @Column(name = "page_size", nullable = false)
    private int pageSize;

    @Column(name = "total_orders_synced", nullable = false)
    private int totalOrdersSynced;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "last_checkpoint_at", nullable = false)
    private Instant lastCheckpointAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    protected OrderSyncCheckpoint() {
        // JPA requirement
    }

    public OrderSyncCheckpoint(
            UUID id,
            UUID tenantId,
            UUID sellerAccountId,
            String marketplace,
            String syncStatus,
            String cursorToken,
            int lastOffset,
            int pageSize,
            int totalOrdersSynced
    ) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        this.sellerAccountId = Objects.requireNonNull(sellerAccountId, "sellerAccountId não pode ser nulo");
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace não pode ser nulo");
        this.syncStatus = syncStatus != null ? syncStatus : "IN_PROGRESS";
        this.cursorToken = cursorToken;
        this.lastOffset = Math.max(0, lastOffset);
        this.pageSize = pageSize > 0 ? pageSize : 50;
        this.totalOrdersSynced = Math.max(0, totalOrdersSynced);
        Instant now = Instant.now();
        this.startedAt = now;
        this.lastCheckpointAt = now;
    }

    public void updateProgress(String newCursorToken, int newOffset, int addedCount) {
        this.cursorToken = newCursorToken;
        this.lastOffset = newOffset;
        this.totalOrdersSynced += addedCount;
        this.syncStatus = "IN_PROGRESS";
        this.lastCheckpointAt = Instant.now();
        this.errorMessage = null;
    }

    public void complete() {
        this.syncStatus = "COMPLETED";
        this.completedAt = Instant.now();
        this.lastCheckpointAt = Instant.now();
        this.errorMessage = null;
    }

    public void fail(String error) {
        this.syncStatus = "FAILED";
        this.errorMessage = error;
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

    public String getCursorToken() {
        return cursorToken;
    }

    public int getLastOffset() {
        return lastOffset;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalOrdersSynced() {
        return totalOrdersSynced;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getLastCheckpointAt() {
        return lastCheckpointAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
