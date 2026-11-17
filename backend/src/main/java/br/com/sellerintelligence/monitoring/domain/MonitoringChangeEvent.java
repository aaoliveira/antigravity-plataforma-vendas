package br.com.sellerintelligence.monitoring.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "monitoring_change_events")
public class MonitoringChangeEvent {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "snapshot_id")
    private UUID snapshotId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private MonitoringEventType eventType;

    @Column(name = "previous_value", columnDefinition = "TEXT")
    private String previousValue;

    @Column(name = "current_value", columnDefinition = "TEXT")
    private String currentValue;

    @Column(name = "price_delta_amount", precision = 15, scale = 2)
    private BigDecimal priceDeltaAmount;

    @Column(name = "price_delta_percentage", precision = 7, scale = 4)
    private BigDecimal priceDeltaPercentage;

    @Column(name = "event_hash", nullable = false, length = 64)
    private String eventHash;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MonitoringChangeEvent() {}

    public MonitoringChangeEvent(UUID tenantId, UUID itemId, UUID snapshotId, MonitoringEventType eventType,
                                 String previousValue, String currentValue, BigDecimal priceDeltaAmount,
                                 BigDecimal priceDeltaPercentage, String eventHash, Instant detectedAt) {
        this.id = UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.itemId = Objects.requireNonNull(itemId, "itemId cannot be null");
        this.snapshotId = snapshotId;
        this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
        this.previousValue = previousValue;
        this.currentValue = currentValue;
        this.priceDeltaAmount = priceDeltaAmount;
        this.priceDeltaPercentage = priceDeltaPercentage;
        this.eventHash = Objects.requireNonNull(eventHash, "eventHash cannot be null");
        this.detectedAt = detectedAt != null ? detectedAt : Instant.now();
        this.createdAt = Instant.now();
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

    public UUID getSnapshotId() {
        return snapshotId;
    }

    public MonitoringEventType getEventType() {
        return eventType;
    }

    public String getPreviousValue() {
        return previousValue;
    }

    public String getCurrentValue() {
        return currentValue;
    }

    public BigDecimal getPriceDeltaAmount() {
        return priceDeltaAmount;
    }

    public BigDecimal getPriceDeltaPercentage() {
        return priceDeltaPercentage;
    }

    public String getEventHash() {
        return eventHash;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
