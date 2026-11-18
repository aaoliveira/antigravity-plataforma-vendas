package com.sellerintelligence.modules.orderingestion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento no padrão Transactional Outbox para integração assíncrona desacoplada de pedidos.
 * Gravado atomicamente na mesma transação de persistência do pedido.
 */
@Entity
@Table(name = "order_outbox_events")
public class OrderOutboxEvent {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected OrderOutboxEvent() {
        // JPA requirement
    }

    public OrderOutboxEvent(
            UUID id,
            UUID tenantId,
            String eventType,
            String aggregateType,
            UUID aggregateId,
            String payloadJson
    ) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        this.eventType = Objects.requireNonNull(eventType, "eventType não pode ser nulo");
        this.aggregateType = aggregateType != null ? aggregateType : "ORDER";
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId não pode ser nulo");
        this.payloadJson = Objects.requireNonNull(payloadJson, "payloadJson não pode ser nulo");
        this.status = "PENDING";
        this.retryCount = 0;
        this.createdAt = Instant.now();
    }

    public void markPublished() {
        this.status = "PUBLISHED";
        this.processedAt = Instant.now();
        this.errorMessage = null;
    }

    public void markFailed(String error) {
        this.status = "FAILED";
        this.retryCount++;
        this.errorMessage = error;
        this.processedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public String getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
