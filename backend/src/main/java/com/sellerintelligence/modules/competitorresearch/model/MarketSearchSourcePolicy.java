package com.sellerintelligence.modules.competitorresearch.model;

import com.sellerintelligence.modules.marketplaceadapters.model.CapabilityStatus;
import com.sellerintelligence.modules.marketplaceadapters.model.MarketplaceCapability;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Entidade que registra a política oficial de uso, conformidade, limites e permissões
 * de cada fonte ou endpoint de dados de marketplace.
 */
@Entity
@Table(name = "market_search_source_policies")
public class MarketSearchSourcePolicy {

    @Id
    @Column(name = "id", length = 100)
    private String id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "marketplace", nullable = false, length = 50)
    private String marketplace;

    @Enumerated(EnumType.STRING)
    @Column(name = "capability", nullable = false, length = 50)
    private MarketplaceCapability capability;

    @Enumerated(EnumType.STRING)
    @Column(name = "capability_status", nullable = false, length = 50)
    private CapabilityStatus capabilityStatus;

    @Column(name = "authorization_requirement", nullable = false, length = 100)
    private String authorizationRequirement;

    @Column(name = "endpoint_pattern", nullable = false, length = 500)
    private String endpointPattern;

    @Column(name = "documentation_url", nullable = false, length = 1000)
    private String documentationUrl;

    @Column(name = "rate_limit_per_minute", nullable = false)
    private int rateLimitPerMinute;

    @Column(name = "allows_competitor_search", nullable = false)
    private boolean allowsCompetitorSearch;

    @Column(name = "policy_rules", nullable = false, columnDefinition = "TEXT")
    private String policyRules;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public MarketSearchSourcePolicy() {
    }

    public MarketSearchSourcePolicy(
            String id,
            String name,
            String marketplace,
            MarketplaceCapability capability,
            CapabilityStatus capabilityStatus,
            String authorizationRequirement,
            String endpointPattern,
            String documentationUrl,
            int rateLimitPerMinute,
            boolean allowsCompetitorSearch,
            String policyRules,
            boolean isActive
    ) {
        this.id = id;
        this.name = name;
        this.marketplace = marketplace;
        this.capability = capability;
        this.capabilityStatus = capabilityStatus;
        this.authorizationRequirement = authorizationRequirement;
        this.endpointPattern = endpointPattern;
        this.documentationUrl = documentationUrl;
        this.rateLimitPerMinute = rateLimitPerMinute;
        this.allowsCompetitorSearch = allowsCompetitorSearch;
        this.policyRules = policyRules;
        this.isActive = isActive;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMarketplace() {
        return marketplace;
    }

    public void setMarketplace(String marketplace) {
        this.marketplace = marketplace;
    }

    public MarketplaceCapability getCapability() {
        return capability;
    }

    public void setCapability(MarketplaceCapability capability) {
        this.capability = capability;
    }

    public CapabilityStatus getCapabilityStatus() {
        return capabilityStatus;
    }

    public void setCapabilityStatus(CapabilityStatus capabilityStatus) {
        this.capabilityStatus = capabilityStatus;
    }

    public String getAuthorizationRequirement() {
        return authorizationRequirement;
    }

    public void setAuthorizationRequirement(String authorizationRequirement) {
        this.authorizationRequirement = authorizationRequirement;
    }

    public String getEndpointPattern() {
        return endpointPattern;
    }

    public void setEndpointPattern(String endpointPattern) {
        this.endpointPattern = endpointPattern;
    }

    public String getDocumentationUrl() {
        return documentationUrl;
    }

    public void setDocumentationUrl(String documentationUrl) {
        this.documentationUrl = documentationUrl;
    }

    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(int rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public boolean isAllowsCompetitorSearch() {
        return allowsCompetitorSearch;
    }

    public void setAllowsCompetitorSearch(boolean allowsCompetitorSearch) {
        this.allowsCompetitorSearch = allowsCompetitorSearch;
    }

    public String getPolicyRules() {
        return policyRules;
    }

    public void setPolicyRules(String policyRules) {
        this.policyRules = policyRules;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
