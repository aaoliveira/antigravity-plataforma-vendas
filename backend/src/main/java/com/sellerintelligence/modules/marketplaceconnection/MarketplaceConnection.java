package com.sellerintelligence.modules.marketplaceconnection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade tenant-scoped representando uma integração com marketplace (Mercado Livre, Shopee, etc.).
 * Protegida por integridade referencial estrita e PostgreSQL Row-Level Security (RLS).
 */
@Entity
@Table(name = "marketplace_connections")
public class MarketplaceConnection {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "marketplace", nullable = false, length = 50)
    private String marketplace;

    @Column(name = "seller_account_id", nullable = false, length = 100)
    private String sellerAccountId;

    @Column(name = "account_name", nullable = false, length = 255)
    private String accountName;

    @Column(name = "encrypted_access_token", columnDefinition = "TEXT")
    private String encryptedAccessToken;

    @Column(name = "encrypted_refresh_token", columnDefinition = "TEXT")
    private String encryptedRefreshToken;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Column(name = "authorized_scopes", length = 255)
    private String authorizedScopes;

    @Column(name = "last_refreshed_at")
    private Instant lastRefreshedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MarketplaceConnection() {
        // JPA requirement
    }

    public MarketplaceConnection(
            UUID id,
            UUID tenantId,
            String marketplace,
            String sellerAccountId,
            String accountName,
            String encryptedAccessToken,
            String encryptedRefreshToken,
            String status
    ) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace não pode ser nulo");
        this.sellerAccountId = Objects.requireNonNull(sellerAccountId, "sellerAccountId não pode ser nulo");
        this.accountName = Objects.requireNonNull(accountName, "accountName não pode ser nulo");
        this.encryptedAccessToken = encryptedAccessToken;
        this.encryptedRefreshToken = encryptedRefreshToken;
        this.status = status != null ? status : "ACTIVE";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getMarketplace() {
        return marketplace;
    }

    public String getSellerAccountId() {
        return sellerAccountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
        this.updatedAt = Instant.now();
    }

    public String getEncryptedAccessToken() {
        return encryptedAccessToken;
    }

    public void setEncryptedAccessToken(String encryptedAccessToken) {
        this.encryptedAccessToken = encryptedAccessToken;
        this.updatedAt = Instant.now();
    }

    public String getEncryptedRefreshToken() {
        return encryptedRefreshToken;
    }

    public void setEncryptedRefreshToken(String encryptedRefreshToken) {
        this.encryptedRefreshToken = encryptedRefreshToken;
        this.updatedAt = Instant.now();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Instant getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(Instant tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
        this.updatedAt = Instant.now();
    }

    public String getAuthorizedScopes() {
        return authorizedScopes;
    }

    public void setAuthorizedScopes(String authorizedScopes) {
        this.authorizedScopes = authorizedScopes;
        this.updatedAt = Instant.now();
    }

    public Instant getLastRefreshedAt() {
        return lastRefreshedAt;
    }

    public void setLastRefreshedAt(Instant lastRefreshedAt) {
        this.lastRefreshedAt = lastRefreshedAt;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MarketplaceConnection that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
