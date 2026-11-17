package br.com.sellerintelligence.financial.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkuCostRepository extends JpaRepository<SkuCost, UUID> {

    List<SkuCost> findByTenantIdAndSkuOrderByEffectiveFromDesc(UUID tenantId, String sku);

    @Query("SELECT c FROM SkuCost c WHERE c.tenantId = :tenantId AND c.sku = :sku " +
           "AND c.effectiveFrom <= :targetDate " +
           "AND (c.effectiveTo IS NULL OR c.effectiveTo > :targetDate) " +
           "ORDER BY c.effectiveFrom DESC")
    List<SkuCost> findValidCostAtDate(
            @Param("tenantId") UUID tenantId,
            @Param("sku") String sku,
            @Param("targetDate") Instant targetDate
    );

    default Optional<SkuCost> findEffectiveCost(UUID tenantId, String sku, Instant targetDate) {
        List<SkuCost> list = findValidCostAtDate(tenantId, sku, targetDate);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
