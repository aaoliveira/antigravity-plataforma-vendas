package com.sellerintelligence.modules.productmonitoring.entity;

import com.sellerintelligence.modules.productmonitoring.model.JobExecutionStatus;
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
 * Entidade JPA de auditoria para execuções e cancelamentos de jobs de monitoramento.
 */
@Entity
@Table(name = "monitoring_job_executions")
public class MonitoringJobExecutionRecord {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "item_id")
    private UUID itemId;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "job_type", nullable = false, length = 50)
    private String jobType;

    @Column(name = "marketplace", nullable = false, length = 50)
    private String marketplace;

    @Column(name = "source_policy_id", nullable = false, length = 100)
    private String sourcePolicyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", nullable = false, length = 50)
    private JobExecutionStatus executionStatus;

    @Column(name = "skip_reason", length = 255)
    private String skipReason;

    @Column(name = "executed_at", nullable = false, updatable = false)
    private Instant executedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    protected MonitoringJobExecutionRecord() {
    }

    public MonitoringJobExecutionRecord(
            UUID id,
            UUID tenantId,
            UUID itemId,
            UUID groupId,
            String jobType,
            String marketplace,
            String sourcePolicyId,
            JobExecutionStatus executionStatus,
            String skipReason,
            Instant executedAt,
            Long durationMs
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.itemId = itemId;
        this.groupId = groupId;
        this.jobType = jobType != null ? jobType : "ITEM_SNAPSHOT";
        this.marketplace = marketplace != null ? marketplace : "UNKNOWN";
        this.sourcePolicyId = sourcePolicyId != null ? sourcePolicyId : "DEFAULT";
        this.executionStatus = Objects.requireNonNull(executionStatus, "executionStatus cannot be null");
        this.skipReason = skipReason;
        this.executedAt = executedAt != null ? executedAt : Instant.now();
        this.durationMs = durationMs;
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

    public UUID getGroupId() {
        return groupId;
    }

    public String getJobType() {
        return jobType;
    }

    public String getMarketplace() {
        return marketplace;
    }

    public String getSourcePolicyId() {
        return sourcePolicyId;
    }

    public JobExecutionStatus getExecutionStatus() {
        return executionStatus;
    }

    public String getSkipReason() {
        return skipReason;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }
}
