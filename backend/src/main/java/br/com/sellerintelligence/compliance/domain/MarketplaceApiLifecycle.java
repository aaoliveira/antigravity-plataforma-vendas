package br.com.sellerintelligence.compliance.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "marketplace_api_lifecycles")
public class MarketplaceApiLifecycle {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Marketplace marketplace;

    @Column(name = "api_name", nullable = false, length = 100)
    private String apiName;

    @Column(nullable = false, length = 50)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ApiLifecycleStatus status;

    @Column(name = "sunset_date")
    private OffsetDateTime sunsetDate;

    @Column(name = "migration_guide_url", length = 1000)
    private String migrationGuideUrl;

    @Column(name = "last_audited_at", nullable = false)
    private OffsetDateTime lastAuditedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public MarketplaceApiLifecycle() {
    }

    public MarketplaceApiLifecycle(
            UUID id,
            Marketplace marketplace,
            String apiName,
            String version,
            ApiLifecycleStatus status,
            OffsetDateTime sunsetDate,
            String migrationGuideUrl) {
        this.id = id != null ? id : UUID.randomUUID();
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace must not be null");
        this.apiName = Objects.requireNonNull(apiName, "apiName must not be null");
        this.version = Objects.requireNonNull(version, "version must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.sunsetDate = sunsetDate;
        this.migrationGuideUrl = migrationGuideUrl;
        this.lastAuditedAt = OffsetDateTime.now();
        this.createdAt = OffsetDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.lastAuditedAt == null) {
            this.lastAuditedAt = OffsetDateTime.now();
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

    public String getApiName() { return apiName; }
    public void setApiName(String apiName) { this.apiName = apiName; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public ApiLifecycleStatus getStatus() { return status; }
    public void setStatus(ApiLifecycleStatus status) { this.status = status; }

    public OffsetDateTime getSunsetDate() { return sunsetDate; }
    public void setSunsetDate(OffsetDateTime sunsetDate) { this.sunsetDate = sunsetDate; }

    public String getMigrationGuideUrl() { return migrationGuideUrl; }
    public void setMigrationGuideUrl(String migrationGuideUrl) { this.migrationGuideUrl = migrationGuideUrl; }

    public OffsetDateTime getLastAuditedAt() { return lastAuditedAt; }
    public void setLastAuditedAt(OffsetDateTime lastAuditedAt) { this.lastAuditedAt = lastAuditedAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
