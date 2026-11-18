package com.sellerintelligence.modules.marketintelligence;

import com.sellerintelligence.modules.marketintelligence.model.MatchStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de correspondência explicável entre um produto canônico e uma observação de mercado.
 * Registra o score geral, scores parciais por dimensão e a explicação legível por humanos.
 */
@Entity
@Table(name = "product_matches")
public class ProductMatch {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "canonical_product_id", nullable = false)
    private UUID canonicalProductId;

    @Column(name = "observation_id", nullable = false)
    private UUID observationId;

    @Column(name = "overall_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal overallScore;

    @Column(name = "gtin_score", precision = 5, scale = 4)
    private BigDecimal gtinScore;

    @Column(name = "title_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal titleScore;

    @Column(name = "brand_score", precision = 5, scale = 4)
    private BigDecimal brandScore;

    @Column(name = "price_score", precision = 5, scale = 4)
    private BigDecimal priceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 50)
    private MatchStatus matchStatus;

    @Column(name = "explanation", nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "threshold_applied", nullable = false, precision = 5, scale = 4)
    private BigDecimal thresholdApplied;

    @Column(name = "matched_at", nullable = false)
    private Instant matchedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ProductMatch() {
        // JPA requirement
    }

    public ProductMatch(
            UUID id,
            UUID tenantId,
            UUID canonicalProductId,
            UUID observationId,
            BigDecimal overallScore,
            BigDecimal gtinScore,
            BigDecimal titleScore,
            BigDecimal brandScore,
            BigDecimal priceScore,
            MatchStatus matchStatus,
            String explanation,
            BigDecimal thresholdApplied,
            Instant matchedAt
    ) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        this.canonicalProductId = Objects.requireNonNull(canonicalProductId, "canonicalProductId não pode ser nulo");
        this.observationId = Objects.requireNonNull(observationId, "observationId não pode ser nulo");
        this.overallScore = Objects.requireNonNull(overallScore, "overallScore não pode ser nulo");
        this.gtinScore = gtinScore;
        this.titleScore = Objects.requireNonNull(titleScore, "titleScore não pode ser nulo");
        this.brandScore = brandScore;
        this.priceScore = priceScore;
        this.matchStatus = Objects.requireNonNull(matchStatus, "matchStatus não pode ser nulo");
        this.explanation = Objects.requireNonNull(explanation, "explanation não pode ser nula");
        this.thresholdApplied = (thresholdApplied != null) ? thresholdApplied : new BigDecimal("0.7500");
        this.matchedAt = (matchedAt != null) ? matchedAt : Instant.now();
        this.createdAt = Instant.now();
    }

    public void updateMatch(
            BigDecimal overallScore,
            BigDecimal gtinScore,
            BigDecimal titleScore,
            BigDecimal brandScore,
            BigDecimal priceScore,
            MatchStatus matchStatus,
            String explanation,
            BigDecimal thresholdApplied,
            Instant matchedAt
    ) {
        this.overallScore = overallScore;
        this.gtinScore = gtinScore;
        this.titleScore = titleScore;
        this.brandScore = brandScore;
        this.priceScore = priceScore;
        this.matchStatus = matchStatus;
        this.explanation = explanation;
        this.thresholdApplied = thresholdApplied;
        this.matchedAt = matchedAt;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getCanonicalProductId() {
        return canonicalProductId;
    }

    public UUID getObservationId() {
        return observationId;
    }

    public BigDecimal getOverallScore() {
        return overallScore;
    }

    public BigDecimal getGtinScore() {
        return gtinScore;
    }

    public BigDecimal getTitleScore() {
        return titleScore;
    }

    public BigDecimal getBrandScore() {
        return brandScore;
    }

    public BigDecimal getPriceScore() {
        return priceScore;
    }

    public MatchStatus getMatchStatus() {
        return matchStatus;
    }

    public String getExplanation() {
        return explanation;
    }

    public BigDecimal getThresholdApplied() {
        return thresholdApplied;
    }

    public Instant getMatchedAt() {
        return matchedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductMatch that = (ProductMatch) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
