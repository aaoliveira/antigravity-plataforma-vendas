package com.sellerintelligence.modules.reporting.entity;

import com.sellerintelligence.modules.reporting.model.ExportAuditAction;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade tenant-scoped para auditoria de ciclo de vida e acessos a relatórios e exportações.
 */
@Entity
@Table(name = "export_audit_logs")
public class ExportAuditLog {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "export_job_id")
    private UUID exportJobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 60)
    private ExportAuditAction action;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ExportAuditLog() {
        // JPA requirement
    }

    public ExportAuditLog(
            UUID id,
            UUID tenantId,
            UUID exportJobId,
            ExportAuditAction action,
            String userId,
            String ipAddress,
            String userAgent,
            String detailsJson
    ) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        this.exportJobId = exportJobId;
        this.action = Objects.requireNonNull(action, "action não pode ser nulo");
        this.userId = Objects.requireNonNull(userId, "userId não pode ser nulo");
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.detailsJson = detailsJson != null ? detailsJson : "{}";
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getExportJobId() {
        return exportJobId;
    }

    public ExportAuditAction getAction() {
        return action;
    }

    public String getUserId() {
        return userId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
