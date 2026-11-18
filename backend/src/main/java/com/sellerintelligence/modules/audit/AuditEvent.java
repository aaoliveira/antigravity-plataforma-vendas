package com.sellerintelligence.modules.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidade de auditoria e conformidade de segurança.
 */
@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "principal_id")
    private UUID principalId;

    @Column(name = "principal_email")
    private String principalEmail;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditEvent() {
    }

    public AuditEvent(
            UUID id,
            String eventType,
            UUID principalId,
            String principalEmail,
            UUID tenantId,
            String ipAddress,
            String userAgent,
            String correlationId,
            String details,
            Instant createdAt
    ) {
        this.id = id;
        this.eventType = eventType;
        this.principalId = principalId;
        this.principalEmail = principalEmail;
        this.tenantId = tenantId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.correlationId = correlationId;
        this.details = details;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public UUID getPrincipalId() {
        return principalId;
    }

    public String getPrincipalEmail() {
        return principalEmail;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
