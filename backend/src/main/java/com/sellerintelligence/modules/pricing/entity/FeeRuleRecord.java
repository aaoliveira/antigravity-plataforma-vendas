package com.sellerintelligence.modules.pricing.entity;

import com.sellerintelligence.modules.pricing.model.FeeSourceType;
import com.sellerintelligence.modules.pricing.model.FeeType;
import com.sellerintelligence.modules.pricing.model.FeeValueType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade que define regras oficiais ou contratuais de comissão, taxa fixa, frete/logística
 * ou outras cobranças por marketplace, modalidade, categoria e faixa de preço.
 */
@Entity
@Table(name = "marketplace_fee_rules")
public class FeeRuleRecord {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "marketplace", nullable = false, length = 50)
    private String marketplace;

    @Column(name = "listing_type", nullable = false, length = 50)
    private String listingType;

    @Column(name = "category_id", length = 100)
    private String categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 50)
    private FeeType feeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_value_type", nullable = false, length = 50)
    private FeeValueType feeValueType;

    @Column(name = "percentage_rate", precision = 8, scale = 4)
    private BigDecimal percentageRate;

    @Column(name = "fixed_amount", precision = 15, scale = 4)
    private BigDecimal fixedAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "min_price_threshold", precision = 15, scale = 4)
    private BigDecimal minPriceThreshold;

    @Column(name = "max_price_threshold", precision = 15, scale = 4)
    private BigDecimal maxPriceThreshold;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private FeeSourceType sourceType;

    @Column(name = "source_reference", nullable = false, columnDefinition = "TEXT")
    private String sourceReference;

    @Column(name = "legal_disclaimer", columnDefinition = "TEXT")
    private String legalDisclaimer;

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FeeRuleRecord() {
        // JPA requirement
    }

    public FeeRuleRecord(
            UUID id,
            UUID tenantId,
            String marketplace,
            String listingType,
            String categoryId,
            FeeType feeType,
            FeeValueType feeValueType,
            BigDecimal percentageRate,
            BigDecimal fixedAmount,
            String currency,
            BigDecimal minPriceThreshold,
            BigDecimal maxPriceThreshold,
            FeeSourceType sourceType,
            String sourceReference,
            String legalDisclaimer,
            Instant effectiveFrom,
            Instant effectiveTo,
            Integer version,
            Boolean isActive,
            String createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace não pode ser nulo").toUpperCase().trim();
        this.listingType = Objects.requireNonNull(listingType, "listingType não pode ser nulo").toUpperCase().trim();
        this.categoryId = (categoryId != null && !categoryId.isBlank()) ? categoryId.trim() : null;
        this.feeType = Objects.requireNonNull(feeType, "feeType não pode ser nulo");
        this.feeValueType = Objects.requireNonNull(feeValueType, "feeValueType não pode ser nulo");
        this.percentageRate = percentageRate != null ? percentageRate.setScale(4, RoundingMode.HALF_UP) : null;
        this.fixedAmount = fixedAmount != null ? fixedAmount.setScale(4, RoundingMode.HALF_UP) : null;
        this.currency = (currency != null && !currency.isBlank()) ? currency.toUpperCase().trim() : "BRL";
        this.minPriceThreshold = minPriceThreshold != null ? minPriceThreshold.setScale(4, RoundingMode.HALF_UP) : null;
        this.maxPriceThreshold = maxPriceThreshold != null ? maxPriceThreshold.setScale(4, RoundingMode.HALF_UP) : null;
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType não pode ser nulo");
        this.sourceReference = Objects.requireNonNull(sourceReference, "sourceReference não pode ser nulo");
        this.legalDisclaimer = legalDisclaimer;
        this.effectiveFrom = Objects.requireNonNull(effectiveFrom, "effectiveFrom não pode ser nulo");
        this.effectiveTo = effectiveTo;
        this.version = (version != null && version > 0) ? version : 1;
        this.isActive = (isActive != null) ? isActive : true;
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy não pode ser nulo");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt não pode ser nulo");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt não pode ser nulo");
    }

    /**
     * Verifica se o preço informado se enquadra na faixa (price tier) definida para a regra.
     * minPriceThreshold e maxPriceThreshold são inclusivos.
     */
    public boolean appliesToPrice(BigDecimal price) {
        if (price == null) {
            return false;
        }
        if (minPriceThreshold != null && price.compareTo(minPriceThreshold) < 0) {
            return false;
        }
        if (maxPriceThreshold != null && price.compareTo(maxPriceThreshold) > 0) {
            return false;
        }
        return true;
    }

    /**
     * Verifica se a regra é vigente no instante fornecido.
     * effectiveFrom <= referenceDate < effectiveTo (ou effectiveTo IS NULL).
     */
    public boolean isEffectiveAt(Instant referenceDate) {
        if (referenceDate == null) {
            return false;
        }
        if (referenceDate.isBefore(effectiveFrom)) {
            return false;
        }
        if (effectiveTo != null && !referenceDate.isBefore(effectiveTo)) {
            return false;
        }
        return Boolean.TRUE.equals(isActive);
    }

    /**
     * Calcula determinísticamente o valor monetário cobrado por esta regra sobre o preço informado.
     * Arredondamento documentado: HALF_UP com escala 4.
     */
    public BigDecimal calculateFeeAmount(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal calculated = BigDecimal.ZERO;

        switch (feeValueType) {
            case PERCENTAGE -> {
                if (percentageRate != null) {
                    calculated = price.multiply(percentageRate)
                            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                }
            }
            case FIXED_AMOUNT -> {
                if (fixedAmount != null) {
                    calculated = fixedAmount.setScale(4, RoundingMode.HALF_UP);
                }
            }
            case PERCENTAGE_PLUS_FIXED -> {
                BigDecimal pct = BigDecimal.ZERO;
                if (percentageRate != null) {
                    pct = price.multiply(percentageRate)
                            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                }
                BigDecimal fix = (fixedAmount != null) ? fixedAmount : BigDecimal.ZERO;
                calculated = pct.add(fix).setScale(4, RoundingMode.HALF_UP);
            }
        }

        return calculated;
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

    public String getMarketplace() {
        return marketplace;
    }

    public String getListingType() {
        return listingType;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public FeeType getFeeType() {
        return feeType;
    }

    public FeeValueType getFeeValueType() {
        return feeValueType;
    }

    public BigDecimal getPercentageRate() {
        return percentageRate;
    }

    public BigDecimal getFixedAmount() {
        return fixedAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getMinPriceThreshold() {
        return minPriceThreshold;
    }

    public BigDecimal getMaxPriceThreshold() {
        return maxPriceThreshold;
    }

    public FeeSourceType getSourceType() {
        return sourceType;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public String getLegalDisclaimer() {
        return legalDisclaimer;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
