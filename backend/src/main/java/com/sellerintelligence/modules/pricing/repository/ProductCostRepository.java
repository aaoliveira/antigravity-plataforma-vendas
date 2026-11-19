package com.sellerintelligence.modules.pricing.repository;

import com.sellerintelligence.modules.pricing.entity.ProductCostRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductCostRepository extends JpaRepository<ProductCostRecord, UUID> {

    List<ProductCostRecord> findByTenantIdAndProductIdOrderByEffectiveFromDesc(UUID tenantId, UUID productId);

    List<ProductCostRecord> findByTenantIdAndProductIdAndIsActiveTrue(UUID tenantId, UUID productId);

    @Query("""
        SELECT c FROM ProductCostRecord c
        WHERE c.tenantId = :tenantId
          AND c.productId = :productId
          AND (:variantId IS NULL AND c.variantId IS NULL OR c.variantId = :variantId)
          AND (:companyId IS NULL AND c.companyId IS NULL OR c.companyId = :companyId)
          AND c.effectiveTo IS NULL
          AND c.isActive = true
    """)
    Optional<ProductCostRecord> findCurrentOpenRecord(
            @Param("tenantId") UUID tenantId,
            @Param("productId") UUID productId,
            @Param("variantId") UUID variantId,
            @Param("companyId") UUID companyId
    );

    @Query("""
        SELECT c FROM ProductCostRecord c
        WHERE c.tenantId = :tenantId
          AND c.productId = :productId
          AND (:variantId IS NULL AND c.variantId IS NULL OR c.variantId = :variantId)
          AND (:companyId IS NULL AND c.companyId IS NULL OR c.companyId = :companyId)
          AND c.effectiveFrom <= :refDate
          AND (c.effectiveTo IS NULL OR c.effectiveTo > :refDate)
          AND c.isActive = true
        ORDER BY c.version DESC
    """)
    List<ProductCostRecord> findCandidateCosts(
            @Param("tenantId") UUID tenantId,
            @Param("productId") UUID productId,
            @Param("variantId") UUID variantId,
            @Param("companyId") UUID companyId,
            @Param("refDate") Instant refDate
    );
}
