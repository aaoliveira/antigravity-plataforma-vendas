package com.sellerintelligence.modules.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade que representa um anúncio próprio concreto em um marketplace específico.
 * Um produto canônico pode ter múltiplos anúncios vinculados (relação 1:N).
 * Preserva o payload original minimizado, schema versionado, origem e timestamps.
 */
@Entity
@Table(name = "marketplace_listings")
public class MarketplaceListing {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "seller_account_id", nullable = false)
    private UUID sellerAccountId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "product_variant_id")
    private UUID productVariantId;

    @Column(name = "marketplace", nullable = false, length = 50)
    private String marketplace;

    @Column(name = "external_listing_id", nullable = false, length = 100)
    private String externalListingId;

    @Column(name = "external_sku", length = 100)
    private String externalSku;

    @Column(name = "external_variation_id", length = 100)
    private String externalVariationId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "price", nullable = false, precision = 15, scale = 4)
    private BigDecimal price;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "permalink", length = 500)
    private String permalink;

    @Column(name = "raw_payload_json", columnDefinition = "TEXT")
    private String rawPayloadJson;

    @Column(name = "schema_version", nullable = false, length = 20)
    private String schemaVersion;

    @Column(name = "origin", nullable = false, length = 100)
    private String origin;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MarketplaceListing() {
        // JPA requirement
    }

    public MarketplaceListing(
            UUID id,
            UUID tenantId,
            UUID sellerAccountId,
            UUID productId,
            UUID productVariantId,
            String marketplace,
            String externalListingId,
            String externalSku,
            String externalVariationId,
            String title,
            BigDecimal price,
            String currency,
            Integer availableQuantity,
            String status,
            String permalink,
            String rawPayloadJson,
            String schemaVersion,
            String origin
    ) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        this.sellerAccountId = Objects.requireNonNull(sellerAccountId, "sellerAccountId não pode ser nulo");
        this.productId = productId;
        this.productVariantId = productVariantId;
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace não pode ser nulo");
        this.externalListingId = Objects.requireNonNull(externalListingId, "externalListingId não pode ser nulo");
        this.externalSku = externalSku;
        this.externalVariationId = externalVariationId;
        this.title = Objects.requireNonNull(title, "title não pode ser nulo");
        this.price = Objects.requireNonNull(price, "price não pode ser nulo");
        this.currency = currency != null ? currency : "BRL";
        this.availableQuantity = availableQuantity != null ? availableQuantity : 0;
        this.status = status != null ? status : "ACTIVE";
        this.permalink = permalink;
        this.rawPayloadJson = rawPayloadJson;
        this.schemaVersion = schemaVersion != null ? schemaVersion : "v1";
        this.origin = Objects.requireNonNull(origin, "origin não pode ser nulo");
        this.syncedAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getSellerAccountId() {
        return sellerAccountId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
        this.updatedAt = Instant.now();
    }

    public UUID getProductVariantId() {
        return productVariantId;
    }

    public void setProductVariantId(UUID productVariantId) {
        this.productVariantId = productVariantId;
        this.updatedAt = Instant.now();
    }

    public String getMarketplace() {
        return marketplace;
    }

    public String getExternalListingId() {
        return externalListingId;
    }

    public String getExternalSku() {
        return externalSku;
    }

    public void setExternalSku(String externalSku) {
        this.externalSku = externalSku;
        this.updatedAt = Instant.now();
    }

    public String getExternalVariationId() {
        return externalVariationId;
    }

    public void setExternalVariationId(String externalVariationId) {
        this.externalVariationId = externalVariationId;
        this.updatedAt = Instant.now();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = Objects.requireNonNull(title, "title não pode ser nulo");
        this.updatedAt = Instant.now();
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = Objects.requireNonNull(price, "price não pode ser nulo");
        this.updatedAt = Instant.now();
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = Objects.requireNonNull(currency, "currency não pode ser nulo");
        this.updatedAt = Instant.now();
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity != null ? availableQuantity : 0;
        this.updatedAt = Instant.now();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = Objects.requireNonNull(status, "status não pode ser nulo");
        this.updatedAt = Instant.now();
    }

    public String getPermalink() {
        return permalink;
    }

    public void setPermalink(String permalink) {
        this.permalink = permalink;
        this.updatedAt = Instant.now();
    }

    public String getRawPayloadJson() {
        return rawPayloadJson;
    }

    public void setRawPayloadJson(String rawPayloadJson) {
        this.rawPayloadJson = rawPayloadJson;
        this.updatedAt = Instant.now();
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
        this.updatedAt = Instant.now();
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
        this.updatedAt = Instant.now();
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(Instant syncedAt) {
        this.syncedAt = Objects.requireNonNull(syncedAt, "syncedAt não pode ser nulo");
        this.updatedAt = Instant.now();
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
        if (!(o instanceof MarketplaceListing that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
