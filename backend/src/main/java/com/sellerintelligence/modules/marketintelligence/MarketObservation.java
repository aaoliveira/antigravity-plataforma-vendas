package com.sellerintelligence.modules.marketintelligence;

import com.sellerintelligence.modules.marketintelligence.model.ItemCondition;
import com.sellerintelligence.modules.marketintelligence.model.MarketObservationSourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade central para observações de mercado públicas, oficiais ou estimadas.
 * Todo registro carrega obrigatoriamente metadados de proveniência (source_type, source_url, timestamps, método e confiança).
 * Valores de vendas, visitas, CTR ou faturamento exigem metodologia e fonte documentadas.
 */
@Entity
@Table(name = "market_observations")
public class MarketObservation {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "observed_listing_id", nullable = false, length = 100)
    private String observedListingId;

    @Column(name = "marketplace", nullable = false, length = 50)
    private String marketplace;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "price", nullable = false, precision = 15, scale = 4)
    private BigDecimal price;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition", nullable = false, length = 50)
    private ItemCondition itemCondition;

    @Column(name = "seller_identifier", length = 255)
    private String sellerIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private MarketObservationSourceType sourceType;

    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl;

    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "method", nullable = false, length = 100)
    private String method;

    @Column(name = "confidence", nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "methodology_description", columnDefinition = "TEXT")
    private String methodologyDescription;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "time_window", length = 50)
    private String timeWindow;

    @Column(name = "gtin", length = 50)
    private String gtin;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "sales_volume", precision = 15, scale = 4)
    private BigDecimal salesVolume;

    @Column(name = "visits_count")
    private Long visitsCount;

    @Column(name = "ctr", precision = 8, scale = 4)
    private BigDecimal ctr;

    @Column(name = "revenue", precision = 15, scale = 4)
    private BigDecimal revenue;

    @Column(name = "minimized_payload", columnDefinition = "TEXT")
    private String minimizedPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MarketObservation() {
        // JPA requirement
    }

    public MarketObservation(
            UUID id,
            UUID tenantId,
            String observedListingId,
            String marketplace,
            String title,
            BigDecimal price,
            String currency,
            ItemCondition itemCondition,
            String sellerIdentifier,
            MarketObservationSourceType sourceType,
            String sourceUrl,
            Instant collectedAt,
            Instant observedAt,
            Instant validUntil,
            String method,
            BigDecimal confidence,
            String methodologyDescription,
            String modelVersion,
            String timeWindow,
            String gtin,
            String brand,
            BigDecimal salesVolume,
            Long visitsCount,
            BigDecimal ctr,
            BigDecimal revenue,
            String minimizedPayload
    ) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        this.observedListingId = Objects.requireNonNull(observedListingId, "observedListingId não pode ser nulo");
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace não pode ser nulo");
        this.title = Objects.requireNonNull(title, "title não pode ser nulo");
        this.price = Objects.requireNonNull(price, "price não pode ser nulo");
        this.currency = (currency != null && !currency.isBlank()) ? currency : "BRL";
        this.itemCondition = (itemCondition != null) ? itemCondition : ItemCondition.NEW;
        this.sellerIdentifier = sellerIdentifier;
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType não pode ser nulo");
        this.sourceUrl = Objects.requireNonNull(sourceUrl, "sourceUrl não pode ser nula");
        this.collectedAt = Objects.requireNonNull(collectedAt, "collectedAt não pode ser nulo");
        this.observedAt = Objects.requireNonNull(observedAt, "observedAt não pode ser nulo");
        this.validUntil = Objects.requireNonNull(validUntil, "validUntil não pode ser nulo");
        this.method = Objects.requireNonNull(method, "method não pode ser nulo");
        this.confidence = (confidence != null) ? confidence : BigDecimal.ONE;
        this.methodologyDescription = methodologyDescription;
        this.modelVersion = modelVersion;
        this.timeWindow = timeWindow;
        this.gtin = gtin;
        this.brand = brand;
        this.salesVolume = salesVolume;
        this.visitsCount = visitsCount;
        this.ctr = ctr;
        this.revenue = revenue;
        this.minimizedPayload = minimizedPayload;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateObservation(
            String title,
            BigDecimal price,
            ItemCondition itemCondition,
            String sellerIdentifier,
            Instant observedAt,
            Instant collectedAt,
            Instant validUntil,
            BigDecimal confidence,
            String gtin,
            String brand,
            BigDecimal salesVolume,
            Long visitsCount,
            BigDecimal ctr,
            BigDecimal revenue,
            String minimizedPayload
    ) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (price != null) {
            this.price = price;
        }
        if (itemCondition != null) {
            this.itemCondition = itemCondition;
        }
        if (sellerIdentifier != null) {
            this.sellerIdentifier = sellerIdentifier;
        }
        if (observedAt != null) {
            this.observedAt = observedAt;
        }
        if (collectedAt != null) {
            this.collectedAt = collectedAt;
        }
        if (validUntil != null) {
            this.validUntil = validUntil;
        }
        if (confidence != null) {
            this.confidence = confidence;
        }
        if (gtin != null) {
            this.gtin = gtin;
        }
        if (brand != null) {
            this.brand = brand;
        }
        if (salesVolume != null) {
            this.salesVolume = salesVolume;
        }
        if (visitsCount != null) {
            this.visitsCount = visitsCount;
        }
        if (ctr != null) {
            this.ctr = ctr;
        }
        if (revenue != null) {
            this.revenue = revenue;
        }
        if (minimizedPayload != null) {
            this.minimizedPayload = minimizedPayload;
        }
        this.updatedAt = Instant.now();
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getObservedListingId() {
        return observedListingId;
    }

    public String getMarketplace() {
        return marketplace;
    }

    public String getTitle() {
        return title;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public ItemCondition getItemCondition() {
        return itemCondition;
    }

    public String getSellerIdentifier() {
        return sellerIdentifier;
    }

    public MarketObservationSourceType getSourceType() {
        return sourceType;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public Instant getCollectedAt() {
        return collectedAt;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public String getMethod() {
        return method;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public String getMethodologyDescription() {
        return methodologyDescription;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public String getTimeWindow() {
        return timeWindow;
    }

    public String getGtin() {
        return gtin;
    }

    public String getBrand() {
        return brand;
    }

    public BigDecimal getSalesVolume() {
        return salesVolume;
    }

    public Long getVisitsCount() {
        return visitsCount;
    }

    public BigDecimal getCtr() {
        return ctr;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public String getMinimizedPayload() {
        return minimizedPayload;
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
        MarketObservation that = (MarketObservation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
