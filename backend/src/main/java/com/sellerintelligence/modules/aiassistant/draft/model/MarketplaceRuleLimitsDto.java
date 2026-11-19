package com.sellerintelligence.modules.aiassistant.draft.model;

public record MarketplaceRuleLimitsDto(
        String marketplace,
        int minTitleLength,
        int maxTitleLength,
        int minDescriptionLength,
        int maxDescriptionLength,
        boolean plainTextOnly,
        String officialRuleSource
) {
    public static MarketplaceRuleLimitsDto from(MarketplaceRuleLimits domain) {
        return new MarketplaceRuleLimitsDto(
                domain.marketplace(),
                domain.minTitleLength(),
                domain.maxTitleLength(),
                domain.minDescriptionLength(),
                domain.maxDescriptionLength(),
                domain.plainTextOnly(),
                domain.officialRuleSource()
        );
    }
}
