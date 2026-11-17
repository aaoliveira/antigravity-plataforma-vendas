package br.com.sellerintelligence.monitoring.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "monitoring_alerts")
public class MonitoringAlert {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "rule_id")
    private UUID ruleId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 50)
    private AlertSeverity severity;

    @Column(name = "dedup_key", nullable = false, length = 128)
    private String dedupKey;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MonitoringAlert() {}

    public MonitoringAlert(UUID tenantId, UUID ruleId, UUID itemId, UUID eventId,
                           String title, String message, AlertSeverity severity,
                           String dedupKey, Instant triggeredAt) {
        this.id = UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.ruleId = ruleId;
        this.itemId = Objects.requireNonNull(itemId, "itemId cannot be null");
        this.eventId = eventId;
        this.title = Objects.requireNonNull(title, "title cannot be null");
        this.message = Objects.requireNonNull(message, "message cannot be null");
        this.severity = severity != null ? severity : AlertSeverity.INFO;
        this.dedupKey = Objects.requireNonNull(dedupKey, "dedupKey cannot be null");
        this.read = false;
        this.triggeredAt = triggeredAt != null ? triggeredAt : Instant.now();
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getRuleId() {
        return ruleId;
    }

    public UUID getItemId() {
        return itemId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public boolean isRead() {
        return read;
    }

    public void markAsRead() {
        this.read = true;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
