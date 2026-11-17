package br.com.sellerintelligence.compliance.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "compliance_incidents")
public class ComplianceIncident {

    @Id
    private UUID id;

    @Column(name = "incident_code", nullable = false, unique = true, length = 50)
    private String incidentCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Marketplace marketplace;

    @Column(name = "policy_id")
    private UUID policyId;

    @Column(name = "policy_change_id")
    private UUID policyChangeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ComplianceRiskLevel severity;

    @Column(name = "affected_features", nullable = false, length = 255)
    private String affectedFeatures;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IncidentStatus status;

    @Column(name = "opened_at", nullable = false)
    private OffsetDateTime openedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(nullable = false, length = 255)
    private String responsible;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "remediation_plan", columnDefinition = "TEXT")
    private String remediationPlan;

    @Column(name = "approved_by", length = 255)
    private String approvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public ComplianceIncident() {
    }

    public ComplianceIncident(
            UUID id,
            String incidentCode,
            Marketplace marketplace,
            UUID policyId,
            UUID policyChangeId,
            ComplianceRiskLevel severity,
            String affectedFeatures,
            IncidentStatus status,
            String responsible,
            String description,
            String evidence,
            String remediationPlan) {
        this.id = id != null ? id : UUID.randomUUID();
        this.incidentCode = Objects.requireNonNull(incidentCode, "incidentCode must not be null");
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace must not be null");
        this.policyId = policyId;
        this.policyChangeId = policyChangeId;
        this.severity = Objects.requireNonNull(severity, "severity must not be null");
        this.affectedFeatures = Objects.requireNonNull(affectedFeatures, "affectedFeatures must not be null");
        this.status = status != null ? status : IncidentStatus.OPEN;
        this.openedAt = OffsetDateTime.now();
        this.responsible = Objects.requireNonNull(responsible, "responsible must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.evidence = evidence;
        this.remediationPlan = remediationPlan;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.openedAt == null) {
            this.openedAt = now;
        }
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public void resolve(String approvedBy, String resolutionNotes) {
        this.status = IncidentStatus.RESOLVED;
        this.resolvedAt = OffsetDateTime.now();
        this.approvedBy = approvedBy;
        if (resolutionNotes != null && !resolutionNotes.isBlank()) {
            this.remediationPlan = (this.remediationPlan != null ? this.remediationPlan + "\n" : "") + "Resolution: " + resolutionNotes;
        }
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getIncidentCode() { return incidentCode; }
    public void setIncidentCode(String incidentCode) { this.incidentCode = incidentCode; }

    public Marketplace getMarketplace() { return marketplace; }
    public void setMarketplace(Marketplace marketplace) { this.marketplace = marketplace; }

    public UUID getPolicyId() { return policyId; }
    public void setPolicyId(UUID policyId) { this.policyId = policyId; }

    public UUID getPolicyChangeId() { return policyChangeId; }
    public void setPolicyChangeId(UUID policyChangeId) { this.policyChangeId = policyChangeId; }

    public ComplianceRiskLevel getSeverity() { return severity; }
    public void setSeverity(ComplianceRiskLevel severity) { this.severity = severity; }

    public String getAffectedFeatures() { return affectedFeatures; }
    public void setAffectedFeatures(String affectedFeatures) { this.affectedFeatures = affectedFeatures; }

    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }

    public OffsetDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(OffsetDateTime openedAt) { this.openedAt = openedAt; }

    public OffsetDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(OffsetDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResponsible() { return responsible; }
    public void setResponsible(String responsible) { this.responsible = responsible; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }

    public String getRemediationPlan() { return remediationPlan; }
    public void setRemediationPlan(String remediationPlan) { this.remediationPlan = remediationPlan; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
