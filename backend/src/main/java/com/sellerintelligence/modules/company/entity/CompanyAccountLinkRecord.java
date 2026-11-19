package com.sellerintelligence.modules.company.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade que materializa o vínculo explícito e versionado entre uma Empresa e uma Conta Conectada de Marketplace.
 * Proíbe ambiguidade: uma conta só pode ter 1 vínculo ativo por vez.
 * Histórico preservado para auditoria e apuração fiscal retroativa.
 */
@Entity
@Table(name = "company_seller_account_links")
public class CompanyAccountLinkRecord {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "seller_account_id", nullable = false)
    private UUID sellerAccountId;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "linked_by", nullable = false)
    private String linkedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CompanyAccountLinkRecord() {
        // JPA
    }

    public CompanyAccountLinkRecord(
            UUID id,
            UUID tenantId,
            UUID companyId,
            UUID sellerAccountId,
            Instant effectiveFrom,
            Instant effectiveTo,
            boolean isActive,
            int version,
            String notes,
            String linkedBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id é obrigatório");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId é obrigatório");
        this.companyId = Objects.requireNonNull(companyId, "companyId é obrigatório");
        this.sellerAccountId = Objects.requireNonNull(sellerAccountId, "sellerAccountId é obrigatório");
        this.effectiveFrom = Objects.requireNonNull(effectiveFrom, "effectiveFrom é obrigatório");
        this.effectiveTo = effectiveTo;
        this.isActive = isActive;
        this.version = version;
        this.notes = notes;
        this.linkedBy = Objects.requireNonNull(linkedBy, "linkedBy é obrigatório");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt é obrigatório");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt é obrigatório");
    }

    public void terminate(Instant terminatedAt) {
        this.effectiveTo = terminatedAt != null ? terminatedAt : Instant.now();
        this.isActive = false;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public UUID getSellerAccountId() {
        return sellerAccountId;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public boolean isActive() {
        return isActive;
    }

    public int getVersion() {
        return version;
    }

    public String getNotes() {
        return notes;
    }

    public String getLinkedBy() {
        return linkedBy;
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
        if (o == null || getClass() != o.getClass()) return false;
        CompanyAccountLinkRecord that = (CompanyAccountLinkRecord) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
