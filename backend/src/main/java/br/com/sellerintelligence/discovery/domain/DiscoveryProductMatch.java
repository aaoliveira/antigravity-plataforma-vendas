package br.com.sellerintelligence.discovery.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "discovery_product_matches")
public class DiscoveryProductMatch {

    @Id
    private UUID id;

    @Column(name = "catalog_item_id", nullable = false)
    private UUID catalogItemId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "marketplace", nullable = false, length = 50)
    private String marketplace;

    @Column(name = "matched_external_id", nullable = false, length = 150)
    private String matchedExternalId;

    @Column(name = "matched_title", nullable = false, length = 500)
    private String matchedTitle;

    @Column(name = "matched_price", precision = 15, scale = 2)
    private BigDecimal matchedPrice;

    @Column(name = "matched_url", columnDefinition = "TEXT")
    private String matchedUrl;

    @Column(name = "similarity_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal similarityScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_source", nullable = false, length = 50)
    private MatchSource matchSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private MatchStatus status;

    @Column(name = "estimated_gross_margin", precision = 7, scale = 4)
    private BigDecimal estimatedGrossMargin;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DiscoveryProductMatch() {}

    public DiscoveryProductMatch(UUID catalogItemId, UUID tenantId, String marketplace,
                                 String matchedExternalId, String matchedTitle, BigDecimal matchedPrice,
                                 String matchedUrl, BigDecimal similarityScore, MatchSource matchSource,
                                 BigDecimal estimatedGrossMargin) {
        this.id = UUID.randomUUID();
        this.catalogItemId = Objects.requireNonNull(catalogItemId, "catalogItemId cannot be null");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace cannot be null");
        this.matchedExternalId = Objects.requireNonNull(matchedExternalId, "matchedExternalId cannot be null");
        this.matchedTitle = Objects.requireNonNull(matchedTitle, "matchedTitle cannot be null");
        this.matchedPrice = matchedPrice;
        this.matchedUrl = matchedUrl;
        this.similarityScore = Objects.requireNonNull(similarityScore, "similarityScore cannot be null");
        this.matchSource = matchSource != null ? matchSource : MatchSource.SEMANTIC_VECTOR;
        this.status = MatchStatus.CANDIDATE; // strictly candidate until human confirms!
        this.estimatedGrossMargin = estimatedGrossMargin;
        this.createdAt = Instant.now();
    }

    public void confirm() {
        this.status = MatchStatus.CONFIRMED;
    }

    public void reject() {
        this.status = MatchStatus.REJECTED;
    }

    public UUID getId() { return id; }
    public UUID getCatalogItemId() { return catalogItemId; }
    public UUID getTenantId() { return tenantId; }
    public String getMarketplace() { return marketplace; }
    public String getMatchedExternalId() { return matchedExternalId; }
    public String getMatchedTitle() { return matchedTitle; }
    public BigDecimal getMatchedPrice() { return matchedPrice; }
    public String getMatchedUrl() { return matchedUrl; }
    public BigDecimal getSimilarityScore() { return similarityScore; }
    public MatchSource getMatchSource() { return matchSource; }
    public MatchStatus getStatus() { return status; }
    public BigDecimal getEstimatedGrossMargin() { return estimatedGrossMargin; }
    public Instant getCreatedAt() { return createdAt; }
}
