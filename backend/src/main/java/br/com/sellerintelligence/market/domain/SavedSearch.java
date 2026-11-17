package br.com.sellerintelligence.market.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "saved_searches")
public class SavedSearch {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "marketplace", nullable = false, length = 50)
    private Marketplace marketplace;

    @Column(name = "query", nullable = false, length = 255)
    private String query;

    @Column(name = "category_filter", length = 100)
    private String categoryFilter;

    @Column(name = "min_price", precision = 15, scale = 2)
    private BigDecimal minPrice;

    @Column(name = "max_price", precision = 15, scale = 2)
    private BigDecimal maxPrice;

    @Column(name = "condition_filter", length = 50)
    private String conditionFilter;

    @Column(name = "brand_filter", length = 100)
    private String brandFilter;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SavedSearch() {}

    public SavedSearch(UUID tenantId, String name, Marketplace marketplace, String query,
                       String categoryFilter, BigDecimal minPrice, BigDecimal maxPrice,
                       String conditionFilter, String brandFilter, String notes) {
        this.id = UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null").trim();
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace cannot be null");
        this.query = Objects.requireNonNull(query, "query cannot be null").trim();
        this.categoryFilter = categoryFilter;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.conditionFilter = conditionFilter;
        this.brandFilter = brandFilter;
        this.notes = notes;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void update(String name, String notes) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        this.notes = notes;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public Marketplace getMarketplace() {
        return marketplace;
    }

    public String getQuery() {
        return query;
    }

    public String getCategoryFilter() {
        return categoryFilter;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public String getConditionFilter() {
        return conditionFilter;
    }

    public String getBrandFilter() {
        return brandFilter;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
