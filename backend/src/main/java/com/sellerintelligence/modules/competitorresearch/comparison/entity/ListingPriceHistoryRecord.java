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
 * Entidade de persistência para o histórico legítimo de preços de um anúncio.
 */
@Entity
@Table(name = "listing_price_history")
public class ListingPriceHistoryRecord {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "marketplace", nullable = false, length = 50)
    private String marketplace;

    @Column(name = "external_listing_id", nullable = false, length = 100)
    private String externalListingId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "price", nullable = false, precision = 15, scale = 4)
    private BigDecimal price;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "shipping_cost", precision = 15, scale = 4)
    private BigDecimal shippingCost;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ListingPriceHistoryRecord() {
        // JPA
    }

    public ListingPriceHistoryRecord(
            UUID id,
            UUID tenantId,
            String marketplace,
            String externalListingId,
            String title,
            BigDecimal price,
            String currency,
            BigDecimal shippingCost,
            Instant observedAt,
            Instant collectedAt,
            String sourceType,
            String sourceUrl
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId é obrigatório");
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace é obrigatório");
        this.externalListingId = Objects.requireNonNull(externalListingId, "externalListingId é obrigatório");
        this.title = Objects.requireNonNull(title, "title é obrigatório");
        this.price = Objects.requireNonNull(price, "price é obrigatório");
        this.currency = (currency != null && !currency.isBlank()) ? currency : "BRL";
        this.shippingCost = shippingCost;
        this.observedAt = observedAt != null ? observedAt : Instant.now();
        this.collectedAt = collectedAt != null ? collectedAt : Instant.now();
        this.sourceType = (sourceType != null && !sourceType.isBlank()) ? sourceType : "OFFICIAL";
        this.sourceUrl = sourceUrl;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getMarketplace() { return marketplace; }
    public String getExternalListingId() { return externalListingId; }
    public String getTitle() { return title; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
    public BigDecimal getShippingCost() { return shippingCost; }
    public Instant getObservedAt() { return observedAt; }
    public Instant getCollectedAt() { return collectedAt; }
    public String getSourceType() { return sourceType; }
    public String getSourceUrl() { return sourceUrl; }
    public Instant getCreatedAt() { return createdAt; }
}
