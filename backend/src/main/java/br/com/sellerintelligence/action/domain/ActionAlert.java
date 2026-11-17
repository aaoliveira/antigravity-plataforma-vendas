package br.com.sellerintelligence.action.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "action_alerts")
public class ActionAlert {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "rule_id")
    private UUID ruleId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 32)
    private ActionAlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "domain_type", nullable = false, length = 64)
    private ActionAlertDomain domainType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ActionAlertStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 32)
    private AlertScopeType scopeType;

    @Column(name = "scope_id", length = 128)
    private String scopeId;

    @Column(name = "provenance", nullable = false, length = 32)
    private String provenance;

    @Column(name = "evidence_context", nullable = false, columnDefinition = "TEXT")
    private String evidenceContext;

    @Column(name = "dedup_key", nullable = false)
    private String dedupKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ActionAlert() {}

    public ActionAlert(UUID tenantId, UUID ruleId, String title, String description,
                       ActionAlertSeverity severity, ActionAlertDomain domainType,
                       AlertScopeType scopeType, String scopeId, String provenance,
                       String evidenceContext, String dedupKey) {
        this.id = UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.ruleId = ruleId;
        this.title = Objects.requireNonNull(title, "title cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.severity = severity != null ? severity : ActionAlertSeverity.WARNING;
        this.domainType = domainType != null ? domainType : ActionAlertDomain.FINANCIAL;
        this.status = ActionAlertStatus.NEW;
        this.scopeType = scopeType != null ? scopeType : AlertScopeType.ORGANIZATION;
        this.scopeId = scopeId;
        this.provenance = provenance != null ? provenance : "CALCULATED";
        this.evidenceContext = Objects.requireNonNull(evidenceContext, "evidenceContext cannot be null");
        this.dedupKey = Objects.requireNonNull(dedupKey, "dedupKey cannot be null");
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void updateStatus(ActionAlertStatus newStatus) {
        this.status = Objects.requireNonNull(newStatus, "newStatus cannot be null");
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getRuleId() { return ruleId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public ActionAlertSeverity getSeverity() { return severity; }
    public ActionAlertDomain getDomainType() { return domainType; }
    public ActionAlertStatus getStatus() { return status; }
    public AlertScopeType getScopeType() { return scopeType; }
    public String getScopeId() { return scopeId; }
    public String getProvenance() { return provenance; }
    public String getEvidenceContext() { return evidenceContext; }
    public String getDedupKey() { return dedupKey; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
