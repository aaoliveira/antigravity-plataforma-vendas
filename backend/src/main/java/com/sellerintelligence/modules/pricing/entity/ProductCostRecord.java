package com.sellerintelligence.modules.pricing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade que armazena custos unitários diretos e percentuais de despesas e impostos por produto/variante/empresa.
 * Possui controle estrito de vigência (effectiveFrom, effectiveTo) e versionamento incremental para auditoria e histórico.
 */
@Entity
@Table(name = "product_costs")
public class ProductCostRecord {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Column(name = "acquisition_cost", nullable = false, precision = 15, scale = 4)
    private BigDecimal acquisitionCost;

    @Column(name = "packaging_cost", nullable = false, precision = 15, scale = 4)
    private BigDecimal packagingCost;

    @Column(name = "additional_cost", nullable = false, precision = 15, scale = 4)
    private BigDecimal additionalCost;

    @Column(name = "subsidized_shipping_cost", nullable = false, precision = 15, scale = 4)
    private BigDecimal subsidizedShippingCost;

    @Column(name = "tax_rate_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal taxRatePercent;

    @Column(name = "other_expenses_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal otherExpensesPercent;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProductCostRecord() {
        // JPA requirement
    }

    public ProductCostRecord(
            UUID id,
            UUID tenantId,
            UUID productId,
            UUID variantId,
            UUID companyId,
            String currency,
            String unit,
            BigDecimal acquisitionCost,
            BigDecimal packagingCost,
            BigDecimal additionalCost,
            BigDecimal subsidizedShippingCost,
            BigDecimal taxRatePercent,
            BigDecimal otherExpensesPercent,
            Instant effectiveFrom,
            Instant effectiveTo,
            Integer version,
            Boolean isActive,
            String createdBy,
            String notes,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        this.productId = Objects.requireNonNull(productId, "productId não pode ser nulo");
        this.variantId = variantId;
        this.companyId = companyId;
        this.currency = (currency != null && !currency.isBlank()) ? currency.toUpperCase().trim() : "BRL";
        this.unit = (unit != null && !unit.isBlank()) ? unit.toUpperCase().trim() : "UN";
        this.acquisitionCost = normalizeScale(acquisitionCost);
        this.packagingCost = normalizeScale(packagingCost);
        this.additionalCost = normalizeScale(additionalCost);
        this.subsidizedShippingCost = normalizeScale(subsidizedShippingCost);
        this.taxRatePercent = normalizeScale(taxRatePercent);
        this.otherExpensesPercent = normalizeScale(otherExpensesPercent);
        this.effectiveFrom = Objects.requireNonNull(effectiveFrom, "effectiveFrom não pode ser nulo");
        this.effectiveTo = effectiveTo;
        this.version = (version != null && version > 0) ? version : 1;
        this.isActive = (isActive != null) ? isActive : true;
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy não pode ser nulo");
        this.notes = notes;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt não pode ser nulo");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt não pode ser nulo");
    }

    private static BigDecimal normalizeScale(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Retorna a soma de todos os custos diretos unitários:
     * aquisição + embalagem + custos adicionais + frete subsidiado.
     */
    public BigDecimal getTotalDirectUnitCost() {
        return acquisitionCost
                .add(packagingCost)
                .add(additionalCost)
                .add(subsidizedShippingCost)
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Encerra a vigência deste registro marcando effectiveTo = newEffectiveTo e atualizando timestamp.
     */
    public void closeValidity(Instant newEffectiveTo) {
        this.effectiveTo = Objects.requireNonNull(newEffectiveTo, "newEffectiveTo não pode ser nulo");
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getProductId() {
        return productId;
    }

    public UUID getVariantId() {
        return variantId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getCurrency() {
        return currency;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getAcquisitionCost() {
        return acquisitionCost;
    }

    public BigDecimal getPackagingCost() {
        return packagingCost;
    }

    public BigDecimal getAdditionalCost() {
        return additionalCost;
    }

    public BigDecimal getSubsidizedShippingCost() {
        return subsidizedShippingCost;
    }

    public BigDecimal getTaxRatePercent() {
        return taxRatePercent;
    }

    public BigDecimal getOtherExpensesPercent() {
        return otherExpensesPercent;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public Integer getVersion() {
        return version;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
