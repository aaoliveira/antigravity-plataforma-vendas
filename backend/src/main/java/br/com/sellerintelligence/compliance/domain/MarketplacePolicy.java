package br.com.sellerintelligence.compliance.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "marketplace_policies")
public class MarketplacePolicy {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Marketplace marketplace;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false, length = 50)
    private PolicyType policyType;

    @Column(name = "policy_name", nullable = false, length = 255)
    private String policyName;

    @Column(name = "official_url", nullable = false, length = 1000)
    private String officialUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private PolicySourceType sourceType;

    @Column(name = "policy_version", length = 50)
    private String policyVersion;

    @Column(name = "effective_date")
    private OffsetDateTime effectiveDate;

    @Column(name = "last_checked_at", nullable = false)
    private OffsetDateTime lastCheckedAt;

    @Column(name = "last_changed_at", nullable = false)
    private OffsetDateTime lastChangedAt;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PolicyStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 50)
    private ComplianceRiskLevel riskLevel;

    @Column(name = "affected_feature", length = 100)
    private String affectedFeature;

    @Column(columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public MarketplacePolicy() {
    }

    public MarketplacePolicy(
            UUID id,
            Marketplace marketplace,
            PolicyType policyType,
            String policyName,
            String officialUrl,
            PolicySourceType sourceType,
            String policyVersion,
            OffsetDateTime effectiveDate,
            String contentHash,
            String rawContent,
            PolicyStatus status,
            ComplianceRiskLevel riskLevel,
            String affectedFeature,
            String evidence) {
        this.id = id != null ? id : UUID.randomUUID();
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace must not be null");
        this.policyType = Objects.requireNonNull(policyType, "policyType must not be null");
        this.policyName = Objects.requireNonNull(policyName, "policyName must not be null");
        this.officialUrl = Objects.requireNonNull(officialUrl, "officialUrl must not be null");
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        this.policyVersion = policyVersion;
        this.effectiveDate = effectiveDate;
        this.contentHash = Objects.requireNonNull(contentHash, "contentHash must not be null");
        this.rawContent = rawContent;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.riskLevel = Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        this.affectedFeature = affectedFeature;
        this.evidence = evidence;
        OffsetDateTime now = OffsetDateTime.now();
        this.lastCheckedAt = now;
        this.lastChangedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.lastCheckedAt == null) {
            this.lastCheckedAt = now;
        }
        if (this.lastChangedAt == null) {
            this.lastChangedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Marketplace getMarketplace() { return marketplace; }
    public void setMarketplace(Marketplace marketplace) { this.marketplace = marketplace; }

    public PolicyType getPolicyType() { return policyType; }
    public void setPolicyType(PolicyType policyType) { this.policyType = policyType; }

    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }

    public String getOfficialUrl() { return officialUrl; }
    public void setOfficialUrl(String officialUrl) { this.officialUrl = officialUrl; }

    public PolicySourceType getSourceType() { return sourceType; }
    public void setSourceType(PolicySourceType sourceType) { this.sourceType = sourceType; }

    public String getPolicyVersion() { return policyVersion; }
    public void setPolicyVersion(String policyVersion) { this.policyVersion = policyVersion; }

    public OffsetDateTime getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(OffsetDateTime effectiveDate) { this.effectiveDate = effectiveDate; }

    public OffsetDateTime getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(OffsetDateTime lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }

    public OffsetDateTime getLastChangedAt() { return lastChangedAt; }
    public void setLastChangedAt(OffsetDateTime lastChangedAt) { this.lastChangedAt = lastChangedAt; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public String getRawContent() { return rawContent; }
    public void setRawContent(String rawContent) { this.rawContent = rawContent; }

    public PolicyStatus getStatus() { return status; }
    public void setStatus(PolicyStatus status) { this.status = status; }

    public ComplianceRiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(ComplianceRiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public String getAffectedFeature() { return affectedFeature; }
    public void setAffectedFeature(String affectedFeature) { this.affectedFeature = affectedFeature; }

    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
