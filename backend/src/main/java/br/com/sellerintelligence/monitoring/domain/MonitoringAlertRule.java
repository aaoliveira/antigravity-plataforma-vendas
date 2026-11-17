package br.com.sellerintelligence.monitoring.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "monitoring_alert_rules")
public class MonitoringAlertRule {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "watchlist_id")
    private UUID watchlistId;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private MonitoringEventType eventType;

    @Column(name = "min_price_delta_percentage", precision = 7, scale = 4)
    private BigDecimal minPriceDeltaPercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 20)
    private AlertDirection direction;

    @Column(name = "cooldown_minutes", nullable = false)
    private int cooldownMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 50)
    private AlertChannel channel;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MonitoringAlertRule() {}

    public MonitoringAlertRule(UUID tenantId, UUID watchlistId, String name, MonitoringEventType eventType,
                               BigDecimal minPriceDeltaPercentage, AlertDirection direction, int cooldownMinutes,
                               AlertChannel channel) {
        this.id = UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.watchlistId = watchlistId;
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.eventType = eventType != null ? eventType : MonitoringEventType.ANY;
        this.minPriceDeltaPercentage = minPriceDeltaPercentage;
        this.direction = direction != null ? direction : AlertDirection.ANY;
        this.cooldownMinutes = cooldownMinutes > 0 ? cooldownMinutes : 1440;
        this.channel = channel != null ? channel : AlertChannel.IN_APP;
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getWatchlistId() {
        return watchlistId;
    }

    public String getName() {
        return name;
    }

    public MonitoringEventType getEventType() {
        return eventType;
    }

    public BigDecimal getMinPriceDeltaPercentage() {
        return minPriceDeltaPercentage;
    }

    public AlertDirection getDirection() {
        return direction;
    }

    public int getCooldownMinutes() {
        return cooldownMinutes;
    }

    public AlertChannel getChannel() {
        return channel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
