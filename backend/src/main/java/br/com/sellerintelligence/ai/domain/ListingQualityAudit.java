package br.com.sellerintelligence.ai.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "listing_quality_audits")
public class ListingQualityAudit {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "overall_score", nullable = false)
    private Integer overallScore;

    @Column(name = "title_score", nullable = false)
    private Integer titleScore;

    @Column(name = "description_score", nullable = false)
    private Integer descriptionScore;

    @Column(name = "attributes_score", nullable = false)
    private Integer attributesScore;

    @Column(name = "images_score", nullable = false)
    private Integer imagesScore;

    @Column(name = "criteria_breakdown_json", nullable = false, columnDefinition = "TEXT")
    private String criteriaBreakdownJson;

    @Column(name = "actionable_recommendations_json", nullable = false, columnDefinition = "TEXT")
    private String actionableRecommendationsJson;

    @Column(name = "missing_required_attributes_json", nullable = false, columnDefinition = "TEXT")
    private String missingRequiredAttributesJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ListingQualityAudit() {
    }

    public ListingQualityAudit(
            UUID tenantId,
            UUID listingId,
            Integer overallScore,
            Integer titleScore,
            Integer descriptionScore,
            Integer attributesScore,
            Integer imagesScore,
            String criteriaBreakdownJson,
            String actionableRecommendationsJson,
            String missingRequiredAttributesJson
    ) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.listingId = listingId;
        this.overallScore = overallScore;
        this.titleScore = titleScore;
        this.descriptionScore = descriptionScore;
        this.attributesScore = attributesScore;
        this.imagesScore = imagesScore;
        this.criteriaBreakdownJson = criteriaBreakdownJson;
        this.actionableRecommendationsJson = actionableRecommendationsJson;
        this.missingRequiredAttributesJson = missingRequiredAttributesJson;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getListingId() {
        return listingId;
    }

    public Integer getOverallScore() {
        return overallScore;
    }

    public Integer getTitleScore() {
        return titleScore;
    }

    public Integer getDescriptionScore() {
        return descriptionScore;
    }

    public Integer getAttributesScore() {
        return attributesScore;
    }

    public Integer getImagesScore() {
        return imagesScore;
    }

    public String getCriteriaBreakdownJson() {
        return criteriaBreakdownJson;
    }

    public String getActionableRecommendationsJson() {
        return actionableRecommendationsJson;
    }

    public String getMissingRequiredAttributesJson() {
        return missingRequiredAttributesJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
