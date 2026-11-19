package com.sellerintelligence.modules.company.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de persistência que representa uma Empresa ou Unidade de Negócio (PJ).
 * Escopada obrigatoriamente por tenantId e protegida por RLS.
 */
@Entity
@Table(name = "companies")
public class CompanyRecord {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "corporate_name", nullable = false, length = 255)
    private String corporateName;

    @Column(name = "trade_name", length = 255)
    private String tradeName;

    @Column(name = "cnpj", nullable = false, length = 14)
    private String cnpj;

    @Column(name = "state_registration", length = 50)
    private String stateRegistration;

    @Column(name = "tax_regime", length = 50)
    private String taxRegime;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CompanyRecord() {
        // JPA
    }

    public CompanyRecord(
            UUID id,
            UUID tenantId,
            String corporateName,
            String tradeName,
            String cnpj,
            String stateRegistration,
            String taxRegime,
            boolean isActive,
            String notes,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id é obrigatório");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId é obrigatório");
        this.corporateName = Objects.requireNonNull(corporateName, "corporateName é obrigatório");
        this.cnpj = Objects.requireNonNull(cnpj, "cnpj é obrigatório");
        this.tradeName = tradeName;
        this.stateRegistration = stateRegistration;
        this.taxRegime = taxRegime;
        this.isActive = isActive;
        this.notes = notes;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt é obrigatório");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt é obrigatório");
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getCorporateName() {
        return corporateName;
    }

    public void setCorporateName(String corporateName) {
        this.corporateName = corporateName;
        this.updatedAt = Instant.now();
    }

    public String getTradeName() {
        return tradeName;
    }

    public void setTradeName(String tradeName) {
        this.tradeName = tradeName;
        this.updatedAt = Instant.now();
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
        this.updatedAt = Instant.now();
    }

    public String getStateRegistration() {
        return stateRegistration;
    }

    public void setStateRegistration(String stateRegistration) {
        this.stateRegistration = stateRegistration;
        this.updatedAt = Instant.now();
    }

    public String getTaxRegime() {
        return taxRegime;
    }

    public void setTaxRegime(String taxRegime) {
        this.taxRegime = taxRegime;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
        this.updatedAt = Instant.now();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
        if (o == null || getClass() != o.getClass()) return false;
        CompanyRecord that = (CompanyRecord) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
