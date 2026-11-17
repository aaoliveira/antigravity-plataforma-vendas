package br.com.sellerintelligence.discovery.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "discovery_catalog_items")
public class DiscoveryCatalogItem {

    @Id
    private UUID id;

    @Column(name = "catalog_id", nullable = false)
    private UUID catalogId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 500)
    private String name;

    @Column(name = "supplier_sku", length = 150)
    private String supplierSku;

    @Column(name = "ean", length = 50)
    private String ean;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cost_price", precision = 15, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "weight_kg", precision = 10, scale = 3)
    private BigDecimal weightKg;

    @Column(name = "dimensions_cm", length = 100)
    private String dimensionsCm;

    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_method", nullable = false, length = 50)
    private ExtractionMethod extractionMethod;

    @Column(name = "overall_confidence", nullable = false, precision = 5, scale = 4)
    private BigDecimal overallConfidence;

    @Column(name = "confidence_details", columnDefinition = "TEXT")
    private String confidenceDetails;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 50)
    private ItemReviewStatus reviewStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DiscoveryCatalogItem() {}

    public DiscoveryCatalogItem(UUID catalogId, UUID tenantId, String name, String supplierSku,
                                String ean, String description, BigDecimal costPrice,
                                BigDecimal weightKg, String dimensionsCm,
                                ExtractionMethod extractionMethod, BigDecimal overallConfidence,
                                String confidenceDetails) {
        this.id = UUID.randomUUID();
        this.catalogId = Objects.requireNonNull(catalogId, "catalogId cannot be null");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.supplierSku = supplierSku;
        this.ean = ean;
        this.description = description;
        this.costPrice = costPrice;
        this.currency = "BRL";
        this.weightKg = weightKg;
        this.dimensionsCm = dimensionsCm;
        this.extractionMethod = extractionMethod != null ? extractionMethod : ExtractionMethod.PDF_TEXT;
        this.overallConfidence = overallConfidence != null ? overallConfidence : BigDecimal.ONE;
        this.confidenceDetails = confidenceDetails;
        this.reviewStatus = ItemReviewStatus.PENDING_REVIEW;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void updateAndConfirm(String name, String supplierSku, String ean, String description,
                                 BigDecimal costPrice, BigDecimal weightKg, String dimensionsCm) {
        if (name != null && !name.isBlank()) this.name = name;
        if (supplierSku != null) this.supplierSku = supplierSku;
        if (ean != null) this.ean = ean;
        if (description != null) this.description = description;
        if (costPrice != null) this.costPrice = costPrice;
        if (weightKg != null) this.weightKg = weightKg;
        if (dimensionsCm != null) this.dimensionsCm = dimensionsCm;
        this.reviewStatus = ItemReviewStatus.REVIEWED;
        this.updatedAt = Instant.now();
    }

    public void reject() {
        this.reviewStatus = ItemReviewStatus.REJECTED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCatalogId() { return catalogId; }
    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getSupplierSku() { return supplierSku; }
    public String getEan() { return ean; }
    public String getDescription() { return description; }
    public BigDecimal getCostPrice() { return costPrice; }
    public String getCurrency() { return currency; }
    public BigDecimal getWeightKg() { return weightKg; }
    public String getDimensionsCm() { return dimensionsCm; }
    public ExtractionMethod getExtractionMethod() { return extractionMethod; }
    public BigDecimal getOverallConfidence() { return overallConfidence; }
    public String getConfidenceDetails() { return confidenceDetails; }
    public ItemReviewStatus getReviewStatus() { return reviewStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
