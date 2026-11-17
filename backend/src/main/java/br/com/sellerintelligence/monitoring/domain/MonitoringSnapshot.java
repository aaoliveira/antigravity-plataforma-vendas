package br.com.sellerintelligence.monitoring.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "monitoring_snapshots")
public class MonitoringSnapshot {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "marketplace", nullable = false, length = 50)
    private Marketplace marketplace;

    @Column(name = "external_id", nullable = false, length = 150)
    private String externalId;

    @Column(name = "price", precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "original_price", precision = 15, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "listing_status", nullable = false, length = 50)
    private String listingStatus;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "seller_nickname", length = 255)
    private String sellerNickname;

    @Column(name = "seller_reputation", length = 100)
    private String sellerReputation;

    @Column(name = "free_shipping")
    private Boolean freeShipping;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "collection_status", nullable = false, length = 50)
    private CollectionStatus collectionStatus;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "source", nullable = false, length = 100)
    private String source;

    @Column(name = "provenance", nullable = false, length = 50)
    private String provenance;

    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MonitoringSnapshot() {}

    public MonitoringSnapshot(UUID tenantId, UUID itemId, Marketplace marketplace, String externalId,
                              BigDecimal price, BigDecimal originalPrice, String currency, String listingStatus,
                              String title, String sellerNickname, String sellerReputation, Boolean freeShipping,
                              String thumbnailUrl, CollectionStatus collectionStatus, String failureReason,
                              String source, String provenance, Instant collectedAt) {
        this.id = UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.itemId = Objects.requireNonNull(itemId, "itemId cannot be null");
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace cannot be null");
        this.externalId = Objects.requireNonNull(externalId, "externalId cannot be null");
        this.price = price;
        this.originalPrice = originalPrice;
        this.currency = currency != null ? currency : "BRL";
        this.listingStatus = listingStatus != null ? listingStatus : "ACTIVE";
        this.title = title;
        this.sellerNickname = sellerNickname;
        this.sellerReputation = sellerReputation;
        this.freeShipping = freeShipping;
        this.thumbnailUrl = thumbnailUrl;
        this.collectionStatus = collectionStatus != null ? collectionStatus : CollectionStatus.SUCCESS;
        this.failureReason = failureReason;
        this.source = source != null ? source : "OFFICIAL_API";
        this.provenance = provenance != null ? provenance : "OBSERVED";
        this.collectedAt = collectedAt != null ? collectedAt : Instant.now();
        this.createdAt = Instant.now();
    }

    public static MonitoringSnapshot failure(UUID tenantId, UUID itemId, Marketplace marketplace,
                                             String externalId, String failureReason, Instant collectedAt) {
        return new MonitoringSnapshot(
                tenantId, itemId, marketplace, externalId,
                null, null, "BRL", "COLLECTION_FAILED",
                null, null, null, null, null,
                CollectionStatus.COLLECTION_FAILED, failureReason,
                "OFFICIAL_CONNECTOR", "UNAVAILABLE", collectedAt
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getItemId() {
        return itemId;
    }

    public Marketplace getMarketplace() {
        return marketplace;
    }

    public String getExternalId() {
        return externalId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public String getListingStatus() {
        return listingStatus;
    }

    public String getTitle() {
        return title;
    }

    public String getSellerNickname() {
        return sellerNickname;
    }

    public String getSellerReputation() {
        return sellerReputation;
    }

    public Boolean getFreeShipping() {
        return freeShipping;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public CollectionStatus getCollectionStatus() {
        return collectionStatus;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getSource() {
        return source;
    }

    public String getProvenance() {
        return provenance;
    }

    public Instant getCollectedAt() {
        return collectedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
