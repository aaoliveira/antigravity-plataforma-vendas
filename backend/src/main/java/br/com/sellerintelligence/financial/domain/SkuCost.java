package br.com.sellerintelligence.financial.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sku_costs")
public class SkuCost {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 120)
    private String sku;

    @Column(name = "cost_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal costPrice;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(nullable = false, length = 50)
    private String origin;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SkuCost() {}

    public SkuCost(UUID id, UUID tenantId, String sku, BigDecimal costPrice, String currency,
                   String unit, String origin, Instant effectiveFrom, Instant effectiveTo, String notes) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.sku = sku;
        this.costPrice = costPrice;
        this.currency = currency != null ? currency : "BRL";
        this.unit = unit != null ? unit : "UN";
        this.origin = origin != null ? origin : "MANUAL";
        this.effectiveFrom = effectiveFrom != null ? effectiveFrom : Instant.now();
        this.effectiveTo = effectiveTo;
        this.notes = notes;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void closeValidity(Instant effectiveTo) {
        this.effectiveTo = effectiveTo;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getSku() {
        return sku;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public String getUnit() {
        return unit;
    }

    public String getOrigin() {
        return origin;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
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
