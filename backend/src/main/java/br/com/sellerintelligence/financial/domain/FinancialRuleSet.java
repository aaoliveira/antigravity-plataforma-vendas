package br.com.sellerintelligence.financial.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "financial_rule_sets")
public class FinancialRuleSet {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(length = 50)
    private String marketplace;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "ruleSet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<FinancialRuleComponent> components = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FinancialRuleSet() {}

    public FinancialRuleSet(UUID id, UUID tenantId, UUID companyId, UUID storeId, String marketplace,
                            String name, Integer version, Instant effectiveFrom, Instant effectiveTo,
                            boolean active) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.companyId = companyId;
        this.storeId = storeId;
        this.marketplace = marketplace;
        this.name = name;
        this.version = version != null ? version : 1;
        this.effectiveFrom = effectiveFrom != null ? effectiveFrom : Instant.now();
        this.effectiveTo = effectiveTo;
        this.active = active;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addComponent(FinancialRuleComponent component) {
        component.setRuleSet(this);
        this.components.add(component);
    }

    public void closeValidity(Instant effectiveTo) {
        this.effectiveTo = effectiveTo;
        this.active = false;
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

    public UUID getStoreId() {
        return storeId;
    }

    public String getMarketplace() {
        return marketplace;
    }

    public String getName() {
        return name;
    }

    public Integer getVersion() {
        return version;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public boolean isActive() {
        return active;
    }

    public List<FinancialRuleComponent> getComponents() {
        return components;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
