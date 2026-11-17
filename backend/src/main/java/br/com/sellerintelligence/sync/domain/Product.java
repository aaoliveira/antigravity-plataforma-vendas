package br.com.sellerintelligence.sync.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "sku", nullable = false, length = 120)
    private String sku;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "brand", length = 255)
    private String brand;

    @Column(name = "category", length = 255)
    private String category;

    @Column(name = "barcode", length = 100)
    private String barcode;

    @Column(name = "cost_price", precision = 15, scale = 2)
    private BigDecimal costPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "provenance", nullable = false, length = 50)
    private DataProvenance provenance;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Product() {}

    public Product(UUID tenantId, UUID storeId, String sku, String title,
                   String brand, String category, String barcode, BigDecimal costPrice,
                   DataProvenance provenance) {
        this.id = UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.storeId = Objects.requireNonNull(storeId, "storeId cannot be null");
        this.sku = Objects.requireNonNull(sku, "sku cannot be null").trim();
        this.title = Objects.requireNonNull(title, "title cannot be null").trim();
        this.brand = brand != null && !brand.isBlank() ? brand.trim() : null;
        this.category = category != null && !category.isBlank() ? category.trim() : null;
        this.barcode = barcode != null && !barcode.isBlank() ? barcode.trim() : null;
        this.costPrice = costPrice;
        this.provenance = provenance != null ? provenance : DataProvenance.IMPORTED;
        this.createdAt = Instant.now();
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

    public String getSku() {
        return sku;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = Objects.requireNonNull(title, "title cannot be null").trim();
        this.updatedAt = Instant.now();
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand != null && !brand.isBlank() ? brand.trim() : null;
        this.updatedAt = Instant.now();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category != null && !category.isBlank() ? category.trim() : null;
        this.updatedAt = Instant.now();
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode != null && !barcode.isBlank() ? barcode.trim() : null;
        this.updatedAt = Instant.now();
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
        this.updatedAt = Instant.now();
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
