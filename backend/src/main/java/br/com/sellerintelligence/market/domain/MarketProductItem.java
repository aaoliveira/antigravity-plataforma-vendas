package br.com.sellerintelligence.market.domain;

import br.com.sellerintelligence.sync.domain.DataProvenance;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "market_product_items")
public class MarketProductItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id")
    private MarketSearchSnapshot snapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "marketplace", nullable = false, length = 50)
    private Marketplace marketplace;

    @Column(name = "external_item_id", nullable = false, length = 150)
    private String externalItemId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "condition", length = 50)
    private String condition;

    @Column(name = "seller_nickname", length = 255)
    private String sellerNickname;

    @Column(name = "seller_reputation", length = 100)
    private String sellerReputation;

    @Column(name = "permalink", columnDefinition = "TEXT")
    private String permalink;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "category_id", length = 100)
    private String categoryId;

    @Column(name = "category_name", length = 255)
    private String categoryName;

    @Column(name = "brand", length = 255)
    private String brand;

    @Column(name = "free_shipping")
    private Boolean freeShipping;

    @Column(name = "review_score", precision = 3, scale = 2)
    private BigDecimal reviewScore;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "estimated_sales_range", length = 100)
    private String estimatedSalesRange;

    @Column(name = "estimation_method", columnDefinition = "TEXT")
    private String estimationMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "provenance", nullable = false, length = 50)
    private DataProvenance provenance;

    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;

    protected MarketProductItem() {}

    public MarketProductItem(MarketSearchSnapshot snapshot, Marketplace marketplace,
                             String externalItemId, String title, BigDecimal price,
                             String currency, String condition, String sellerNickname,
                             String sellerReputation, String permalink, String thumbnailUrl,
                             String categoryId, String categoryName, String brand,
                             Boolean freeShipping, BigDecimal reviewScore, Integer reviewCount,
                             String estimatedSalesRange, String estimationMethod,
                             DataProvenance provenance, Instant collectedAt) {
        this.id = UUID.randomUUID();
        this.snapshot = snapshot;
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace cannot be null");
        this.externalItemId = Objects.requireNonNull(externalItemId, "externalItemId cannot be null").trim();
        this.title = Objects.requireNonNull(title, "title cannot be null").trim();
        this.price = Objects.requireNonNull(price, "price cannot be null");
        this.currency = currency != null ? currency.toUpperCase() : "BRL";
        this.condition = condition;
        this.sellerNickname = sellerNickname;
        this.sellerReputation = sellerReputation;
        this.permalink = permalink;
        this.thumbnailUrl = thumbnailUrl;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.brand = brand;
        this.freeShipping = freeShipping;
        this.reviewScore = reviewScore;
        this.reviewCount = reviewCount;
        this.estimatedSalesRange = estimatedSalesRange;
        this.estimationMethod = estimationMethod;
        this.provenance = provenance != null ? provenance : DataProvenance.OBSERVED;
        this.collectedAt = collectedAt != null ? collectedAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public MarketSearchSnapshot getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(MarketSearchSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public Marketplace getMarketplace() {
        return marketplace;
    }

    public String getExternalItemId() {
        return externalItemId;
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

    public String getCondition() {
        return condition;
    }

    public String getSellerNickname() {
        return sellerNickname;
    }

    public String getSellerReputation() {
        return sellerReputation;
    }

    public String getPermalink() {
        return permalink;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getBrand() {
        return brand;
    }

    public Boolean getFreeShipping() {
        return freeShipping;
    }

    public BigDecimal getReviewScore() {
        return reviewScore;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public String getEstimatedSalesRange() {
        return estimatedSalesRange;
    }

    public String getEstimationMethod() {
        return estimationMethod;
    }

    public DataProvenance getProvenance() {
        return provenance;
    }

    public Instant getCollectedAt() {
        return collectedAt;
    }
}
