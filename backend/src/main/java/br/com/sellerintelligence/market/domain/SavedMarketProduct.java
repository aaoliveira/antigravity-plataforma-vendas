package br.com.sellerintelligence.market.domain;

import br.com.sellerintelligence.sync.domain.DataProvenance;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "saved_market_products",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_saved_market_products_tenant_item", columnNames = {"tenant_id", "marketplace", "external_item_id"})
        }
)
public class SavedMarketProduct {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

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

    @Column(name = "seller_nickname", length = 255)
    private String sellerNickname;

    @Column(name = "permalink", columnDefinition = "TEXT")
    private String permalink;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "provenance", nullable = false, length = 50)
    private DataProvenance provenance;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SavedMarketProduct() {}

    public SavedMarketProduct(UUID tenantId, Marketplace marketplace, String externalItemId,
                              String title, BigDecimal price, String currency,
                              String sellerNickname, String permalink, String thumbnailUrl,
                              String notes, DataProvenance provenance) {
        this.id = UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace cannot be null");
        this.externalItemId = Objects.requireNonNull(externalItemId, "externalItemId cannot be null").trim();
        this.title = Objects.requireNonNull(title, "title cannot be null").trim();
        this.price = Objects.requireNonNull(price, "price cannot be null");
        this.currency = currency != null ? currency.toUpperCase() : "BRL";
        this.sellerNickname = sellerNickname;
        this.permalink = permalink;
        this.thumbnailUrl = thumbnailUrl;
        this.notes = notes;
        this.provenance = provenance != null ? provenance : DataProvenance.OBSERVED;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void updateNotes(String notes) {
        this.notes = notes;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
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

    public String getSellerNickname() {
        return sellerNickname;
    }

    public String getPermalink() {
        return permalink;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getNotes() {
        return notes;
    }

    public DataProvenance getProvenance() {
        return provenance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
