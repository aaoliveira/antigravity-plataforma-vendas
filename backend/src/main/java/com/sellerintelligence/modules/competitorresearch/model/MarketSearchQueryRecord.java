package com.sellerintelligence.modules.competitorresearch.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Registro de histórico e auditoria de cada consulta de busca de mercado efetuada pelo tenant.
 */
@Entity
@Table(name = "market_search_queries")
public class MarketSearchQueryRecord {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "query_text", nullable = false, length = 255)
    private String queryText;

    @Column(name = "marketplace", length = 50)
    private String marketplace;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "min_price", precision = 15, scale = 4)
    private BigDecimal minPrice;

    @Column(name = "max_price", precision = 15, scale = 4)
    private BigDecimal maxPrice;

    @Column(name = "only_own_catalog", nullable = false)
    private boolean onlyOwnCatalog;

    @Column(name = "source_policy_id", length = 100)
    private String sourcePolicyId;

    @Column(name = "capability_status", nullable = false, length = 50)
    private String capabilityStatus;

    @Column(name = "result_count", nullable = false)
    private int resultCount;

    @Column(name = "execution_time_ms", nullable = false)
    private long executionTimeMs;

    @Column(name = "cached", nullable = false)
    private boolean cached;

    @Column(name = "executed_at", nullable = false, updatable = false)
    private Instant executedAt = Instant.now();

    public MarketSearchQueryRecord() {
    }

    public MarketSearchQueryRecord(
            UUID id,
            UUID tenantId,
            String queryText,
            String marketplace,
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            boolean onlyOwnCatalog,
            String sourcePolicyId,
            String capabilityStatus,
            int resultCount,
            long executionTimeMs,
            boolean cached,
            Instant executedAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.queryText = queryText;
        this.marketplace = marketplace;
        this.category = category;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.onlyOwnCatalog = onlyOwnCatalog;
        this.sourcePolicyId = sourcePolicyId;
        this.capabilityStatus = capabilityStatus;
        this.resultCount = resultCount;
        this.executionTimeMs = executionTimeMs;
        this.cached = cached;
        this.executedAt = executedAt != null ? executedAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public String getMarketplace() {
        return marketplace;
    }

    public void setMarketplace(String marketplace) {
        this.marketplace = marketplace;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public boolean isOnlyOwnCatalog() {
        return onlyOwnCatalog;
    }

    public void setOnlyOwnCatalog(boolean onlyOwnCatalog) {
        this.onlyOwnCatalog = onlyOwnCatalog;
    }

    public String getSourcePolicyId() {
        return sourcePolicyId;
    }

    public void setSourcePolicyId(String sourcePolicyId) {
        this.sourcePolicyId = sourcePolicyId;
    }

    public String getCapabilityStatus() {
        return capabilityStatus;
    }

    public void setCapabilityStatus(String capabilityStatus) {
        this.capabilityStatus = capabilityStatus;
    }

    public int getResultCount() {
        return resultCount;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public boolean isCached() {
        return cached;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }
}
