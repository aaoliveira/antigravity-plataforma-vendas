package br.com.sellerintelligence.compliance.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "marketplace_policy_changes")
public class MarketplacePolicyChange {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private MarketplacePolicy policy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Marketplace marketplace;

    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 50)
    private PolicyChangeType changeType;

    @Column(name = "previous_content", columnDefinition = "TEXT")
    private String previousContent;

    @Column(name = "current_content", columnDefinition = "TEXT")
    private String currentContent;

    @Column(name = "previous_hash", length = 64)
    private String previousHash;

    @Column(name = "current_hash", length = 64)
    private String currentHash;

    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt;

    @Column(name = "effective_date")
    private OffsetDateTime effectiveDate;

    @Column(columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "preliminary_impact", columnDefinition = "TEXT")
    private String preliminaryImpact;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 50)
    private ComplianceRiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ChangeAnalysisStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public MarketplacePolicyChange() {
    }

    public MarketplacePolicyChange(
            UUID id,
            MarketplacePolicy policy,
            Marketplace marketplace,
            String sourceUrl,
            PolicyChangeType changeType,
            String previousContent,
            String currentContent,
            String previousHash,
            String currentHash,
            OffsetDateTime detectedAt,
            OffsetDateTime effectiveDate,
            String evidence,
            String preliminaryImpact,
            ComplianceRiskLevel riskLevel,
            ChangeAnalysisStatus status) {
        this.id = id != null ? id : UUID.randomUUID();
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace must not be null");
        this.sourceUrl = Objects.requireNonNull(sourceUrl, "sourceUrl must not be null");
        this.changeType = Objects.requireNonNull(changeType, "changeType must not be null");
        this.previousContent = previousContent;
        this.currentContent = currentContent;
        this.previousHash = previousHash;
        this.currentHash = currentHash;
        this.detectedAt = detectedAt != null ? detectedAt : OffsetDateTime.now();
        this.effectiveDate = effectiveDate;
        this.evidence = evidence;
        this.preliminaryImpact = preliminaryImpact;
        this.riskLevel = Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        this.status = status != null ? status : ChangeAnalysisStatus.NEW;
        this.createdAt = OffsetDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.detectedAt == null) {
            this.detectedAt = OffsetDateTime.now();
        }
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public MarketplacePolicy getPolicy() { return policy; }
    public void setPolicy(MarketplacePolicy policy) { this.policy = policy; }

    public Marketplace getMarketplace() { return marketplace; }
    public void setMarketplace(Marketplace marketplace) { this.marketplace = marketplace; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public PolicyChangeType getChangeType() { return changeType; }
    public void setChangeType(PolicyChangeType changeType) { this.changeType = changeType; }

    public String getPreviousContent() { return previousContent; }
    public void setPreviousContent(String previousContent) { this.previousContent = previousContent; }

    public String getCurrentContent() { return currentContent; }
    public void setCurrentContent(String currentContent) { this.currentContent = currentContent; }

    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }

    public String getCurrentHash() { return currentHash; }
    public void setCurrentHash(String currentHash) { this.currentHash = currentHash; }

    public OffsetDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(OffsetDateTime detectedAt) { this.detectedAt = detectedAt; }

    public OffsetDateTime getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(OffsetDateTime effectiveDate) { this.effectiveDate = effectiveDate; }

    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }

    public String getPreliminaryImpact() { return preliminaryImpact; }
    public void setPreliminaryImpact(String preliminaryImpact) { this.preliminaryImpact = preliminaryImpact; }

    public ComplianceRiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(ComplianceRiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public ChangeAnalysisStatus getStatus() { return status; }
    public void setStatus(ChangeAnalysisStatus status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
