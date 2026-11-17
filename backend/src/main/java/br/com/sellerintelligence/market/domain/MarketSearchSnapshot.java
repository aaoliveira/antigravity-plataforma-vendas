package br.com.sellerintelligence.market.domain;

import br.com.sellerintelligence.sync.domain.DataProvenance;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "market_search_snapshots")
public class MarketSearchSnapshot {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

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

    @Column(name = "sort_order", length = 50)
    private String sortOrder;

    @Column(name = "total_results", nullable = false)
    private int totalResults;

    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "provenance", nullable = false, length = 50)
    private DataProvenance provenance;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MarketProductItem> items = new ArrayList<>();

    protected MarketSearchSnapshot() {}

    public MarketSearchSnapshot(UUID tenantId, Marketplace marketplace, String query,
                                String categoryFilter, BigDecimal minPrice, BigDecimal maxPrice,
                                String conditionFilter, String brandFilter, String sortOrder,
                                int totalResults, Instant collectedAt, DataProvenance provenance) {
        this.id = UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace cannot be null");
        this.query = Objects.requireNonNull(query, "query cannot be null").trim();
        this.categoryFilter = categoryFilter;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.conditionFilter = conditionFilter;
        this.brandFilter = brandFilter;
        this.sortOrder = sortOrder;
        this.totalResults = totalResults;
        this.collectedAt = collectedAt != null ? collectedAt : Instant.now();
        this.provenance = provenance != null ? provenance : DataProvenance.OBSERVED;
        this.createdAt = Instant.now();
    }

    public void addItem(MarketProductItem item) {
        items.add(item);
        item.setSnapshot(this);
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

    public String getSortOrder() {
        return sortOrder;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public Instant getCollectedAt() {
        return collectedAt;
    }

    public DataProvenance getProvenance() {
        return provenance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<MarketProductItem> getItems() {
        return items;
    }
}
