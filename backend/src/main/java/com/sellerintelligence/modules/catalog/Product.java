package com.sellerintelligence.modules.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade mestra que representa o produto canônico interno do tenant.
 * Um produto canônico pode ter múltiplos anúncios (MarketplaceListing) vinculados em diversos canais.
 * Deduplicação NUNCA é realizada apenas por título.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "canonical_sku", length = 100)
    private String canonicalSku;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Product() {
        // JPA requirement
    }

    public Product(
            UUID id,
            UUID tenantId,
            String title,
            String canonicalSku,
            String brand,
            String category,
            String status
    ) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        this.title = Objects.requireNonNull(title, "title não pode ser nulo");
        this.canonicalSku = canonicalSku;
        this.brand = brand;
        this.category = category;
        this.status = status != null ? status : "ACTIVE";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = Objects.requireNonNull(title, "title não pode ser nulo");
        this.updatedAt = Instant.now();
    }

    public String getCanonicalSku() {
        return canonicalSku;
    }

    public void setCanonicalSku(String canonicalSku) {
        this.canonicalSku = canonicalSku;
        this.updatedAt = Instant.now();
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
        this.updatedAt = Instant.now();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
        this.updatedAt = Instant.now();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = Objects.requireNonNull(status, "status não pode ser nulo");
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
        if (!(o instanceof Product product)) return false;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
