package br.com.sellerintelligence.connector.model;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import br.com.sellerintelligence.tenancy.store.Store;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "marketplace_credentials",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_marketplace_credentials_store", columnNames = {"store_id"})
        }
)
public class MarketplaceCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Marketplace marketplace;

    @Column(name = "encrypted_access_token", nullable = false, columnDefinition = "TEXT")
    private String encryptedAccessToken;

    @Column(name = "encrypted_refresh_token", columnDefinition = "TEXT")
    private String encryptedRefreshToken;

    @Column(name = "external_account_id", nullable = false)
    private String externalAccountId;

    @Column(name = "scopes_consented", columnDefinition = "TEXT")
    private String scopesConsented;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "refresh_expires_at")
    private OffsetDateTime refreshExpiresAt;

    @Column(name = "last_validated_at", nullable = false)
    private OffsetDateTime lastValidatedAt;

    @Column(nullable = false, length = 50)
    private String status = "CONNECTED";

    @Column(nullable = false, length = 50)
    private String environment = "PRODUCTION";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public MarketplaceCredential() {
    }

    public MarketplaceCredential(UUID tenantId, Store store, Marketplace marketplace,
                                 String encryptedAccessToken, String encryptedRefreshToken,
                                 String externalAccountId, String scopesConsented,
                                 OffsetDateTime expiresAt, OffsetDateTime refreshExpiresAt,
                                 String environment) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace must not be null");
        this.encryptedAccessToken = Objects.requireNonNull(encryptedAccessToken, "encryptedAccessToken must not be null");
        this.encryptedRefreshToken = encryptedRefreshToken;
        this.externalAccountId = Objects.requireNonNull(externalAccountId, "externalAccountId must not be null");
        this.scopesConsented = scopesConsented;
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.refreshExpiresAt = refreshExpiresAt;
        this.environment = environment != null ? environment : "PRODUCTION";
        this.status = "CONNECTED";
        this.lastValidatedAt = OffsetDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.lastValidatedAt == null) {
            this.lastValidatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(this.expiresAt);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public Marketplace getMarketplace() {
        return marketplace;
    }

    public void setMarketplace(Marketplace marketplace) {
        this.marketplace = marketplace;
    }

    public String getEncryptedAccessToken() {
        return encryptedAccessToken;
    }

    public void setEncryptedAccessToken(String encryptedAccessToken) {
        this.encryptedAccessToken = encryptedAccessToken;
    }

    public String getEncryptedRefreshToken() {
        return encryptedRefreshToken;
    }

    public void setEncryptedRefreshToken(String encryptedRefreshToken) {
        this.encryptedRefreshToken = encryptedRefreshToken;
    }

    public String getExternalAccountId() {
        return externalAccountId;
    }

    public void setExternalAccountId(String externalAccountId) {
        this.externalAccountId = externalAccountId;
    }

    public String getScopesConsented() {
        return scopesConsented;
    }

    public void setScopesConsented(String scopesConsented) {
        this.scopesConsented = scopesConsented;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public OffsetDateTime getRefreshExpiresAt() {
        return refreshExpiresAt;
    }

    public void setRefreshExpiresAt(OffsetDateTime refreshExpiresAt) {
        this.refreshExpiresAt = refreshExpiresAt;
    }

    public OffsetDateTime getLastValidatedAt() {
        return lastValidatedAt;
    }

    public void setLastValidatedAt(OffsetDateTime lastValidatedAt) {
        this.lastValidatedAt = lastValidatedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
