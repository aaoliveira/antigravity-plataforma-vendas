package com.sellerintelligence.modules.aiassistant.draft.model;

/**
 * Regras oficiais de limite de caracteres e formatação por marketplace validado (P020-BE).
 */
public record MarketplaceRuleLimits(
        String marketplace,
        int minTitleLength,
        int maxTitleLength,
        int minDescriptionLength,
        int maxDescriptionLength,
        boolean plainTextOnly,
        String officialRuleSource
) {
    public boolean isTitleValid(String title) {
        if (title == null) return false;
        int len = title.trim().length();
        return len >= minTitleLength && len <= maxTitleLength;
    }

    public boolean isDescriptionValid(String description) {
        if (description == null) return false;
        int len = description.trim().length();
        return len >= minDescriptionLength && len <= maxDescriptionLength;
    }
}
