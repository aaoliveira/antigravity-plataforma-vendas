package com.sellerintelligence.modules.competitorresearch.comparison.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de persistência para comparações de listings salvas no tenant.
 */
@Entity
@Table(name = "listing_comparisons")
public class ListingComparisonRecord {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "base_listing_id", length = 100)
    private String baseListingId;

    @Column(name = "base_title", nullable = false, length = 500)
    private String baseTitle;

    @Column(name = "base_brand", length = 255)
    private String baseBrand;

    @Column(name = "base_model", length = 255)
    private String baseModel;

    @Column(name = "base_gtin", length = 64)
    private String baseGtin;

    @Column(name = "base_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal basePrice;

    @Column(name = "base_shipping_cost", precision = 15, scale = 4)
    private BigDecimal baseShippingCost;

    @Column(name = "base_observed_at", nullable = false)
    private Instant baseObservedAt;

    @Column(name = "base_marketplace", nullable = false, length = 50)
    private String baseMarketplace;

    @Column(name = "target_listing_id", length = 100)
    private String targetListingId;

    @Column(name = "target_title", nullable = false, length = 500)
    private String targetTitle;

    @Column(name = "target_brand", length = 255)
    private String targetBrand;

    @Column(name = "target_model", length = 255)
    private String targetModel;

    @Column(name = "target_gtin", length = 64)
    private String targetGtin;

    @Column(name = "target_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal targetPrice;

    @Column(name = "target_shipping_cost", precision = 15, scale = 4)
    private BigDecimal targetShippingCost;

    @Column(name = "target_observed_at", nullable = false)
    private Instant targetObservedAt;

    @Column(name = "target_marketplace", nullable = false, length = 50)
    private String targetMarketplace;

    @Column(name = "target_seller_name", length = 255)
    private String targetSellerName;

    @Column(name = "target_source_url", length = 1000)
    private String targetSourceUrl;

    @Column(name = "similarity_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal similarityScore;

    @Column(name = "gtin_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal gtinScore;

    @Column(name = "brand_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal brandScore;

    @Column(name = "model_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal modelScore;

    @Column(name = "title_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal titleScore;

    @Column(name = "attributes_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal attributesScore;

    @Column(name = "is_equivalent", nullable = false)
    private boolean isEquivalent;

    @Column(name = "is_compatible", nullable = false)
    private boolean isCompatible;

    @Column(name = "compatibility_status", nullable = false, length = 32)
    private String compatibilityStatus;

    @Column(name = "incompatibility_reasons_json", columnDefinition = "TEXT")
    private String incompatibilityReasonsJson;

    @Column(name = "price_variation_comparable", nullable = false)
    private boolean priceVariationComparable;

    @Column(name = "price_difference", precision = 15, scale = 4)
    private BigDecimal priceDifference;

    @Column(name = "price_variation_percentage", precision = 7, scale = 4)
    private BigDecimal priceVariationPercentage;

    @Column(name = "time_delta_days")
    private Long timeDeltaDays;

    @Column(name = "explanation", nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ListingComparisonRecord() {
        // JPA
    }

    public ListingComparisonRecord(
            UUID id,
            UUID tenantId,
            String baseListingId,
            String baseTitle,
            String baseBrand,
            String baseModel,
            String baseGtin,
            BigDecimal basePrice,
            BigDecimal baseShippingCost,
            Instant baseObservedAt,
            String baseMarketplace,
            String targetListingId,
            String targetTitle,
            String targetBrand,
            String targetModel,
            String targetGtin,
            BigDecimal targetPrice,
            BigDecimal targetShippingCost,
            Instant targetObservedAt,
            String targetMarketplace,
            String targetSellerName,
            String targetSourceUrl,
            BigDecimal similarityScore,
            BigDecimal gtinScore,
            BigDecimal brandScore,
            BigDecimal modelScore,
            BigDecimal titleScore,
            BigDecimal attributesScore,
            boolean isEquivalent,
            boolean isCompatible,
            String compatibilityStatus,
            String incompatibilityReasonsJson,
            boolean priceVariationComparable,
            BigDecimal priceDifference,
            BigDecimal priceVariationPercentage,
            Long timeDeltaDays,
            String explanation
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId é obrigatório");
        this.baseListingId = baseListingId;
        this.baseTitle = Objects.requireNonNull(baseTitle, "baseTitle é obrigatório");
        this.baseBrand = baseBrand;
        this.baseModel = baseModel;
        this.baseGtin = baseGtin;
        this.basePrice = Objects.requireNonNull(basePrice, "basePrice é obrigatório");
        this.baseShippingCost = baseShippingCost;
        this.baseObservedAt = baseObservedAt != null ? baseObservedAt : Instant.now();
        this.baseMarketplace = baseMarketplace != null ? baseMarketplace : "UNKNOWN";
        this.targetListingId = targetListingId;
        this.targetTitle = Objects.requireNonNull(targetTitle, "targetTitle é obrigatório");
        this.targetBrand = targetBrand;
        this.targetModel = targetModel;
        this.targetGtin = targetGtin;
        this.targetPrice = Objects.requireNonNull(targetPrice, "targetPrice é obrigatório");
        this.targetShippingCost = targetShippingCost;
        this.targetObservedAt = targetObservedAt != null ? targetObservedAt : Instant.now();
        this.targetMarketplace = targetMarketplace != null ? targetMarketplace : "UNKNOWN";
        this.targetSellerName = targetSellerName;
        this.targetSourceUrl = targetSourceUrl;
        this.similarityScore = Objects.requireNonNull(similarityScore);
        this.gtinScore = Objects.requireNonNull(gtinScore);
        this.brandScore = Objects.requireNonNull(brandScore);
        this.modelScore = Objects.requireNonNull(modelScore);
        this.titleScore = Objects.requireNonNull(titleScore);
        this.attributesScore = Objects.requireNonNull(attributesScore);
        this.isEquivalent = isEquivalent;
        this.isCompatible = isCompatible;
        this.compatibilityStatus = compatibilityStatus != null ? compatibilityStatus : "COMPATIBLE";
        this.incompatibilityReasonsJson = incompatibilityReasonsJson;
        this.priceVariationComparable = priceVariationComparable;
        this.priceDifference = priceDifference;
        this.priceVariationPercentage = priceVariationPercentage;
        this.timeDeltaDays = timeDeltaDays;
        this.explanation = Objects.requireNonNull(explanation);
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getBaseListingId() { return baseListingId; }
    public String getBaseTitle() { return baseTitle; }
    public String getBaseBrand() { return baseBrand; }
    public String getBaseModel() { return baseModel; }
    public String getBaseGtin() { return baseGtin; }
    public BigDecimal getBasePrice() { return basePrice; }
    public BigDecimal getBaseShippingCost() { return baseShippingCost; }
    public Instant getBaseObservedAt() { return baseObservedAt; }
    public String getBaseMarketplace() { return baseMarketplace; }
    public String getTargetListingId() { return targetListingId; }
    public String getTargetTitle() { return targetTitle; }
    public String getTargetBrand() { return targetBrand; }
    public String getTargetModel() { return targetModel; }
    public String getTargetGtin() { return targetGtin; }
    public BigDecimal getTargetPrice() { return targetPrice; }
    public BigDecimal getTargetShippingCost() { return targetShippingCost; }
    public Instant getTargetObservedAt() { return targetObservedAt; }
    public String getTargetMarketplace() { return targetMarketplace; }
    public String getTargetSellerName() { return targetSellerName; }
    public String getTargetSourceUrl() { return targetSourceUrl; }
    public BigDecimal getSimilarityScore() { return similarityScore; }
    public BigDecimal getGtinScore() { return gtinScore; }
    public BigDecimal getBrandScore() { return brandScore; }
    public BigDecimal getModelScore() { return modelScore; }
    public BigDecimal getTitleScore() { return titleScore; }
    public BigDecimal getAttributesScore() { return attributesScore; }
    public boolean isEquivalent() { return isEquivalent; }
    public boolean isCompatible() { return isCompatible; }
    public String getCompatibilityStatus() { return compatibilityStatus; }
    public String getIncompatibilityReasonsJson() { return incompatibilityReasonsJson; }
    public boolean isPriceVariationComparable() { return priceVariationComparable; }
    public BigDecimal getPriceDifference() { return priceDifference; }
    public BigDecimal getPriceVariationPercentage() { return priceVariationPercentage; }
    public Long getTimeDeltaDays() { return timeDeltaDays; }
    public String getExplanation() { return explanation; }
    public Instant getCreatedAt() { return createdAt; }
}
