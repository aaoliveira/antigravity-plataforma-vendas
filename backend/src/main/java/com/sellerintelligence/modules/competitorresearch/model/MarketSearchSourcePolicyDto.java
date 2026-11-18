package com.sellerintelligence.modules.competitorresearch.model;

import java.time.Instant;

/**
 * DTO para exposição pública das políticas de integração e conformidade de cada fonte de dados.
 */
public record MarketSearchSourcePolicyDto(
        String id,
        String name,
        String marketplace,
        String capability,
        String capabilityStatus,
        String authorizationRequirement,
        String endpointPattern,
        String documentationUrl,
        int rateLimitPerMinute,
        boolean allowsCompetitorSearch,
        String policyRules,
        boolean isActive
) {
    public static MarketSearchSourcePolicyDto fromEntity(MarketSearchSourcePolicy policy) {
        return new MarketSearchSourcePolicyDto(
                policy.getId(),
                policy.getName(),
                policy.getMarketplace(),
                policy.getCapability().name(),
                policy.getCapabilityStatus().name(),
                policy.getAuthorizationRequirement(),
                policy.getEndpointPattern(),
                policy.getDocumentationUrl(),
                policy.getRateLimitPerMinute(),
                policy.isAllowsCompetitorSearch(),
                policy.getPolicyRules(),
                policy.isActive()
        );
    }
}
