package br.com.sellerintelligence.compliance.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "safe_reactivation_logs")
public class SafeReactivationLog {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Marketplace marketplace;

    @Column(name = "feature_name", nullable = false, length = 100)
    private String featureName;

    @Column(name = "approved_by", nullable = false, length = 255)
    private String approvedBy;

    @Column(name = "approved_at", nullable = false)
    private OffsetDateTime approvedAt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "policy_version", length = 50)
    private String policyVersion;

    @Column(name = "tests_executed", nullable = false, columnDefinition = "TEXT")
    private String testsExecuted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public SafeReactivationLog() {
    }

    public SafeReactivationLog(
            UUID id,
            Marketplace marketplace,
            String featureName,
            String approvedBy,
            String evidence,
            String policyVersion,
            String testsExecuted) {
        this.id = id != null ? id : UUID.randomUUID();
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace must not be null");
        this.featureName = Objects.requireNonNull(featureName, "featureName must not be null");
        this.approvedBy = Objects.requireNonNull(approvedBy, "approvedBy must not be null");
        this.evidence = Objects.requireNonNull(evidence, "evidence must not be null");
        this.policyVersion = policyVersion;
        this.testsExecuted = Objects.requireNonNull(testsExecuted, "testsExecuted must not be null");
        this.approvedAt = OffsetDateTime.now();
        this.createdAt = OffsetDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.approvedAt == null) {
            this.approvedAt = OffsetDateTime.now();
        }
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Marketplace getMarketplace() { return marketplace; }
    public void setMarketplace(Marketplace marketplace) { this.marketplace = marketplace; }

    public String getFeatureName() { return featureName; }
    public void setFeatureName(String featureName) { this.featureName = featureName; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public OffsetDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(OffsetDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }

    public String getPolicyVersion() { return policyVersion; }
    public void setPolicyVersion(String policyVersion) { this.policyVersion = policyVersion; }

    public String getTestsExecuted() { return testsExecuted; }
    public void setTestsExecuted(String testsExecuted) { this.testsExecuted = testsExecuted; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
