package com.sellerintelligence.modules.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Representa uma variante específica do produto canônico interno (ex: voltagem, cor, tamanho).
 * Armazena SKU da variante, código GTIN/EAN e atributos estruturados.
 */
@Entity
@Table(name = "product_variants")
public class ProductVariant {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "variant_sku", length = 100)
    private String variantSku;

    @Column(name = "gtin_barcode", length = 50)
    private String gtinBarcode;

    @Column(name = "attributes_json", columnDefinition = "TEXT")
    private String attributesJson;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProductVariant() {
        // JPA requirement
    }

    public ProductVariant(
            UUID id,
            UUID tenantId,
            UUID productId,
            String variantSku,
            String gtinBarcode,
            String attributesJson,
            String status
    ) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        this.productId = Objects.requireNonNull(productId, "productId não pode ser nulo");
        this.variantSku = variantSku;
        this.gtinBarcode = gtinBarcode;
        this.attributesJson = attributesJson;
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

    public UUID getProductId() {
        return productId;
    }

    public String getVariantSku() {
        return variantSku;
    }

    public void setVariantSku(String variantSku) {
        this.variantSku = variantSku;
        this.updatedAt = Instant.now();
    }

    public String getGtinBarcode() {
        return gtinBarcode;
    }

    public void setGtinBarcode(String gtinBarcode) {
        this.gtinBarcode = gtinBarcode;
        this.updatedAt = Instant.now();
    }

    public String getAttributesJson() {
        return attributesJson;
    }

    public void setAttributesJson(String attributesJson) {
        this.attributesJson = attributesJson;
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
        if (!(o instanceof ProductVariant that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
