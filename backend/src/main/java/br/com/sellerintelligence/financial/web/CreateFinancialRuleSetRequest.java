package br.com.sellerintelligence.financial.web;

import br.com.sellerintelligence.financial.domain.FinancialComponentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateFinancialRuleSetRequest(
        UUID companyId,
        UUID storeId,
        String marketplace,

        @NotBlank(message = "Nome da regra é obrigatório")
        String name,

        Instant effectiveFrom,
        List<RuleComponentItem> components
) {
    public record RuleComponentItem(
            @NotNull(message = "Tipo de componente é obrigatório")
            FinancialComponentType type,

            @NotNull(message = "Valor do componente é obrigatório")
            BigDecimal value,

            BigDecimal thresholdAmount,
            String appliesTo,
            String description
    ) {}
}
