package com.sellerintelligence.modules.productmonitoring.entity;

import com.sellerintelligence.modules.productmonitoring.model.MonitoringGroupStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade JPA para grupo de monitoramento vinculado estritamente a um tenant.
 */
@Entity
@Table(name = "monitoring_groups")
public class MonitoringGroupRecord {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "color", length = 30)
    private String color;

    @Column(name = "min_interval_minutes", nullable = false)
    private int minIntervalMinutes;

    @Column(name = "retention_days", nullable = false)
    private int retentionDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MonitoringGroupStatus status;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MonitoringGroupRecord() {
    }

    public MonitoringGroupRecord(
            UUID id,
            UUID tenantId,
            String name,
            String description,
            String color,
            int minIntervalMinutes,
            int retentionDays,
            MonitoringGroupStatus status,
            Instant deletedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.description = description;
        this.color = color != null ? color : "#3B82F6";
        this.minIntervalMinutes = Math.max(minIntervalMinutes, 15);
        this.retentionDays = Math.max(retentionDays, 7);
        this.status = status != null ? status : MonitoringGroupStatus.ACTIVE;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.updatedAt = Instant.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
        this.updatedAt = Instant.now();
    }

    public int getMinIntervalMinutes() {
        return minIntervalMinutes;
    }

    public void setMinIntervalMinutes(int minIntervalMinutes) {
        this.minIntervalMinutes = Math.max(minIntervalMinutes, 15);
        this.updatedAt = Instant.now();
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = Math.max(retentionDays, 7);
        this.updatedAt = Instant.now();
    }

    public MonitoringGroupStatus getStatus() {
        return status;
    }

    public void setStatus(MonitoringGroupStatus status) {
        this.status = status;
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
