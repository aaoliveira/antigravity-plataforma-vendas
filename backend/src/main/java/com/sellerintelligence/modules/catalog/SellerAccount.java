package com.sellerintelligence.modules.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Representa uma conta de vendedor (loja ou seller profile) em um marketplace homologado.
 * Escopada obrigatoriamente por tenant_id e protegida por RLS.
 */
@Entity
@Table(name = "seller_accounts")
public class SellerAccount {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "marketplace", nullable = false, length = 50)
    private String marketplace;

    @Column(name = "external_account_id", nullable = false, length = 100)
    private String externalAccountId;

    @Column(name = "account_name", nullable = false, length = 255)
    private String accountName;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SellerAccount() {
        // JPA requirement
    }

    public SellerAccount(
            UUID id,
            UUID tenantId,
            String marketplace,
            String externalAccountId,
            String accountName,
            String status
    ) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace não pode ser nulo");
        this.externalAccountId = Objects.requireNonNull(externalAccountId, "externalAccountId não pode ser nulo");
        this.accountName = Objects.requireNonNull(accountName, "accountName não pode ser nulo");
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

    public String getExternalAccountId() {
        return externalAccountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = Objects.requireNonNull(accountName, "accountName não pode ser nulo");
        this.updatedAt = Instant.now();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = Objects.requireNonNull(status, "status não pode ser nulo");
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
        if (!(o instanceof SellerAccount that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
