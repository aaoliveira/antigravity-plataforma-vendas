package com.sellerintelligence.modules.orderingestion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro de erro ou falha irrecuperável na ingestão de pedido na Dead-Letter Queue (DLQ).
 */
@Entity
@Table(name = "order_dlq_records")
public class OrderDlqRecord {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "seller_account_id", nullable = false)
    private UUID sellerAccountId;

    @Column(name = "marketplace", nullable = false, length = 50)
    private String marketplace;

    @Column(name = "external_order_id", length = 100)
    private String externalOrderId;

    @Column(name = "raw_payload_json", columnDefinition = "TEXT")
    private String rawPayloadJson;

    @Column(name = "error_reason", nullable = false, columnDefinition = "TEXT")
    private String errorReason;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrderDlqRecord() {
        // JPA requirement
    }

    public OrderDlqRecord(
            UUID id,
            UUID tenantId,
            UUID sellerAccountId,
            String marketplace,
            String externalOrderId,
            String rawPayloadJson,
            String errorReason
    ) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        this.sellerAccountId = Objects.requireNonNull(sellerAccountId, "sellerAccountId não pode ser nulo");
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace não pode ser nulo");
        this.externalOrderId = externalOrderId;
        this.rawPayloadJson = rawPayloadJson;
        this.errorReason = Objects.requireNonNull(errorReason, "errorReason não pode ser nulo");
        this.retryCount = 0;
        this.status = "UNRESOLVED";
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void incrementRetry(String reason) {
        this.retryCount++;
        this.errorReason = reason;
        this.updatedAt = Instant.now();
    }

    public void markResolved() {
        this.status = "RESOLVED";
        this.updatedAt = Instant.now();
    }

    public void markIgnored() {
        this.status = "IGNORED";
        this.updatedAt = Instant.now();
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

    public String getExternalOrderId() {
        return externalOrderId;
    }

    public String getRawPayloadJson() {
        return rawPayloadJson;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
