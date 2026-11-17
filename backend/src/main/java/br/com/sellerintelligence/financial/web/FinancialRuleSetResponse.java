package br.com.sellerintelligence.financial.web;

import br.com.sellerintelligence.financial.domain.FinancialRuleSet;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FinancialRuleSetResponse(
        UUID id,
        UUID companyId,
        UUID storeId,
        String marketplace,
        String name,
        Integer version,
        Instant effectiveFrom,
        Instant effectiveTo,
        boolean active,
        List<RuleComponentResponse> components
) {
    public record RuleComponentResponse(
            UUID id,
            String componentType,
            BigDecimal componentValue,
            BigDecimal thresholdAmount,
            String appliesTo,
            String description
    ) {}

    public static FinancialRuleSetResponse fromDomain(FinancialRuleSet r) {
        List<RuleComponentResponse> compList = r.getComponents().stream()
                .map(c -> new RuleComponentResponse(
                        c.getId(),
                        c.getComponentType().name(),
                        c.getComponentValue(),
                        c.getThresholdAmount(),
                        c.getAppliesTo(),
                        c.getDescription()
                ))
                .toList();

        return new FinancialRuleSetResponse(
                r.getId(),
                r.getCompanyId(),
                r.getStoreId(),
                r.getMarketplace(),
                r.getName(),
                r.getVersion(),
                r.getEffectiveFrom(),
                r.getEffectiveTo(),
                r.isActive(),
                compList
        );
    }
}
