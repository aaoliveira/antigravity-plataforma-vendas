package com.sellerintelligence.modules.competitorresearch.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro de cache de resultados de pesquisa por tenant para evitar chamadas redundantes upstream
 * e respeitar as quotas de rate limit das APIs oficiais.
 */
@Entity
@Table(name = "market_search_cache")
public class MarketSearchCacheRecord {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "cache_key", nullable = false, length = 255)
    private String cacheKey;

    @Column(name = "source_policy_id", length = 100)
    private String sourcePolicyId;

    @Column(name = "results_payload_json", nullable = false, columnDefinition = "TEXT")
    private String resultsPayloadJson;

    @Column(name = "result_count", nullable = false)
    private int resultCount;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public MarketSearchCacheRecord() {
    }

    public MarketSearchCacheRecord(
            UUID id,
            UUID tenantId,
            String cacheKey,
            String sourcePolicyId,
            String resultsPayloadJson,
            int resultCount,
            Instant expiresAt,
            Instant createdAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.cacheKey = cacheKey;
        this.sourcePolicyId = sourcePolicyId;
        this.resultsPayloadJson = resultsPayloadJson;
        this.resultCount = resultCount;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
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

    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public String getSourcePolicyId() {
        return sourcePolicyId;
    }

    public void setSourcePolicyId(String sourcePolicyId) {
        this.sourcePolicyId = sourcePolicyId;
    }

    public String getResultsPayloadJson() {
        return resultsPayloadJson;
    }

    public void setResultsPayloadJson(String resultsPayloadJson) {
        this.resultsPayloadJson = resultsPayloadJson;
    }

    public int getResultCount() {
        return resultCount;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
