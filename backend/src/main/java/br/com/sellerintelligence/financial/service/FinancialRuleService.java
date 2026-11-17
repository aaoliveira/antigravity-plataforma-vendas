package br.com.sellerintelligence.financial.service;

import br.com.sellerintelligence.financial.domain.FinancialComponentType;
import br.com.sellerintelligence.financial.domain.FinancialRuleComponent;
import br.com.sellerintelligence.financial.domain.FinancialRuleSet;
import br.com.sellerintelligence.financial.domain.FinancialRuleSetRepository;
import br.com.sellerintelligence.tenancy.domain.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class FinancialRuleService {

    private final FinancialRuleSetRepository ruleSetRepository;

    public FinancialRuleService(FinancialRuleSetRepository ruleSetRepository) {
        this.ruleSetRepository = ruleSetRepository;
    }

    public record ComponentConfig(
            FinancialComponentType type,
            BigDecimal value,
            BigDecimal thresholdAmount,
            String appliesTo,
            String description
    ) {}

    public FinancialRuleSet createOrVersionRuleSet(
            UUID companyId, UUID storeId, String marketplace, String name,
            Instant effectiveFrom, List<ComponentConfig> components
    ) {
        UUID tenantId = TenantContext.getTenantId();
        Instant now = Instant.now();
        Instant start = effectiveFrom != null ? effectiveFrom : now;

        List<FinancialRuleSet> existing = ruleSetRepository.findByTenantIdAndNameOrderByVersionDesc(tenantId, name);
        int nextVersion = 1;
        if (!existing.isEmpty()) {
            FinancialRuleSet latest = existing.get(0);
            nextVersion = latest.getVersion() + 1;
            latest.closeValidity(start);
            ruleSetRepository.save(latest);
        }

        FinancialRuleSet ruleSet = new FinancialRuleSet(
                UUID.randomUUID(),
                tenantId,
                companyId,
                storeId,
                marketplace,
                name,
                nextVersion,
                start,
                null,
                true
        );

        if (components != null) {
            for (ComponentConfig c : components) {
                FinancialRuleComponent comp = new FinancialRuleComponent(
                        UUID.randomUUID(),
                        ruleSet,
                        c.type(),
                        c.value(),
                        c.thresholdAmount(),
                        c.appliesTo(),
                        c.description()
                );
                ruleSet.addComponent(comp);
            }
        }

        return ruleSetRepository.save(ruleSet);
    }

    @Transactional(readOnly = true)
    public List<FinancialRuleSet> listRules() {
        UUID tenantId = TenantContext.getTenantId();
        return ruleSetRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public Optional<FinancialRuleSet> findBestMatchingRule(String marketplace, Instant targetDate) {
        UUID tenantId = TenantContext.getTenantId();
        return ruleSetRepository.findBestMatchingRule(tenantId, marketplace, targetDate != null ? targetDate : Instant.now());
    }

    @Transactional(readOnly = true)
    public Optional<FinancialRuleSet> getRuleById(UUID ruleId) {
        UUID tenantId = TenantContext.getTenantId();
        return ruleSetRepository.findById(ruleId)
                .filter(r -> r.getTenantId().equals(tenantId));
    }
}
