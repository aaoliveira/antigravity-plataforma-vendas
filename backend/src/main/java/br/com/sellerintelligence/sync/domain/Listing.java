package br.com.sellerintelligence.sync.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "listings")
public class Listing {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "product_id")
    private UUID productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "marketplace", nullable = false, length = 50)
    private Marketplace marketplace;

    @Column(name = "external_listing_id", nullable = false, length = 150)
    private String externalListingId;

    @Column(name = "sku", length = 120)
    private String sku;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "available_quantity")
    private Integer availableQuantity;

    @Column(name = "sold_quantity")
    private Integer soldQuantity;

    @Column(name = "listing_type", length = 100)
    private String listingType;

    @Column(name = "permalink", columnDefinition = "TEXT")
    private String permalink;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "provenance", nullable = false, length = 50)
    private DataProvenance provenance;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "origin_updated_at")
    private Instant originUpdatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Listing() {}

    public Listing(UUID tenantId, UUID storeId, UUID productId, Marketplace marketplace,
                   String externalListingId, String sku, String title, String status,
                   BigDecimal price, String currency, Integer availableQuantity, Integer soldQuantity,
                   String listingType, String permalink, String thumbnailUrl,
                   DataProvenance provenance, String rawPayload, Instant originUpdatedAt) {
        this.id = UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.storeId = Objects.requireNonNull(storeId, "storeId cannot be null");
        this.productId = productId;
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace cannot be null");
        this.externalListingId = Objects.requireNonNull(externalListingId, "externalListingId cannot be null").trim();
        this.sku = sku != null && !sku.isBlank() ? sku.trim() : null;
        this.title = Objects.requireNonNull(title, "title cannot be null").trim();
        this.status = Objects.requireNonNull(status, "status cannot be null").toUpperCase();
        this.price = Objects.requireNonNull(price, "price cannot be null");
        this.currency = currency != null && !currency.isBlank() ? currency.toUpperCase() : "BRL";
        this.availableQuantity = availableQuantity;
        this.soldQuantity = soldQuantity;
        this.listingType = listingType;
        this.permalink = permalink;
        this.thumbnailUrl = thumbnailUrl;
        this.provenance = provenance != null ? provenance : DataProvenance.IMPORTED;
        this.rawPayload = rawPayload;
        this.lastSyncedAt = Instant.now();
        this.originUpdatedAt = originUpdatedAt;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void update(String title, String status, BigDecimal price, Integer availableQuantity,
                       Integer soldQuantity, String listingType, String permalink, String thumbnailUrl,
                       String rawPayload, Instant originUpdatedAt) {
        if (title != null && !title.isBlank()) {
            this.title = title.trim();
        }
        if (status != null && !status.isBlank()) {
            this.status = status.toUpperCase();
        }
        if (price != null) {
            this.price = price;
        }
        this.availableQuantity = availableQuantity;
        this.soldQuantity = soldQuantity;
        if (listingType != null) {
            this.listingType = listingType;
        }
        if (permalink != null) {
            this.permalink = permalink;
        }
        if (thumbnailUrl != null) {
            this.thumbnailUrl = thumbnailUrl;
        }
        if (rawPayload != null) {
            this.rawPayload = rawPayload;
        }
        this.originUpdatedAt = originUpdatedAt;
        this.lastSyncedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getStoreId() {
        return storeId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
        this.updatedAt = Instant.now();
    }

    public Marketplace getMarketplace() {
        return marketplace;
    }

    public String getExternalListingId() {
        return externalListingId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku != null && !sku.isBlank() ? sku.trim() : null;
        this.updatedAt = Instant.now();
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public Integer getSoldQuantity() {
        return soldQuantity;
    }

    public String getListingType() {
        return listingType;
    }

    public String getPermalink() {
        return permalink;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public DataProvenance getProvenance() {
        return provenance;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public Instant getOriginUpdatedAt() {
        return originUpdatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
