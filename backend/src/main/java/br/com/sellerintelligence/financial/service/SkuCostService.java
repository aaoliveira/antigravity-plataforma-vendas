package br.com.sellerintelligence.financial.service;

import br.com.sellerintelligence.financial.domain.SkuCost;
import br.com.sellerintelligence.financial.domain.SkuCostRepository;
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
public class SkuCostService {

    private final SkuCostRepository skuCostRepository;

    public SkuCostService(SkuCostRepository skuCostRepository) {
        this.skuCostRepository = skuCostRepository;
    }

    public SkuCost registerCost(String sku, BigDecimal costPrice, String currency, String unit,
                                String origin, Instant effectiveFrom, String notes) {
        UUID tenantId = TenantContext.getTenantId();
        Instant now = Instant.now();
        Instant start = effectiveFrom != null ? effectiveFrom : now;

        // Close any currently open validity for this SKU
        List<SkuCost> activeCosts = skuCostRepository.findValidCostAtDate(tenantId, sku, start);
        for (SkuCost active : activeCosts) {
            active.closeValidity(start);
            skuCostRepository.save(active);
        }

        SkuCost newCost = new SkuCost(
                UUID.randomUUID(),
                tenantId,
                sku,
                costPrice,
                currency,
                unit,
                origin,
                start,
                null,
                notes
        );
        return skuCostRepository.save(newCost);
    }

    @Transactional(readOnly = true)
    public Optional<SkuCost> getEffectiveCost(String sku, Instant targetDate) {
        UUID tenantId = TenantContext.getTenantId();
        Instant date = targetDate != null ? targetDate : Instant.now();
        return skuCostRepository.findEffectiveCost(tenantId, sku, date);
    }

    @Transactional(readOnly = true)
    public List<SkuCost> getCostHistory(String sku) {
        UUID tenantId = TenantContext.getTenantId();
        return skuCostRepository.findByTenantIdAndSkuOrderByEffectiveFromDesc(tenantId, sku);
    }
}
