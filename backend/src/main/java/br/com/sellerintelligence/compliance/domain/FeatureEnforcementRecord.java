package br.com.sellerintelligence.compliance.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "feature_enforcement_records",
       uniqueConstraints = @UniqueConstraint(name = "uk_feature_enforcement", columnNames = {"marketplace", "feature_name"}))
public class FeatureEnforcementRecord {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Marketplace marketplace;

    @Column(name = "feature_name", nullable = false, length = 100)
    private String featureName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PolicyStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "incident_id")
    private UUID incidentId;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public FeatureEnforcementRecord() {
    }

    public FeatureEnforcementRecord(
            UUID id,
            Marketplace marketplace,
            String featureName,
            PolicyStatus status,
            String reason,
            UUID incidentId) {
        this.id = id != null ? id : UUID.randomUUID();
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace must not be null");
        this.featureName = Objects.requireNonNull(featureName, "featureName must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
        this.incidentId = incidentId;
        this.updatedAt = OffsetDateTime.now();
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        this.updatedAt = OffsetDateTime.now();
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Marketplace getMarketplace() { return marketplace; }
    public void setMarketplace(Marketplace marketplace) { this.marketplace = marketplace; }

    public String getFeatureName() { return featureName; }
    public void setFeatureName(String featureName) { this.featureName = featureName; }

    public PolicyStatus getStatus() { return status; }
    public void setStatus(PolicyStatus status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public UUID getIncidentId() { return incidentId; }
    public void setIncidentId(UUID incidentId) { this.incidentId = incidentId; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
