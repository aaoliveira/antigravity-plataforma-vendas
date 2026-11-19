package com.sellerintelligence.modules.pricing.repository;

import com.sellerintelligence.modules.pricing.entity.FeeRuleRecord;
import com.sellerintelligence.modules.pricing.model.FeeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeeRuleRepository extends JpaRepository<FeeRuleRecord, UUID> {

    List<FeeRuleRecord> findByTenantIdAndMarketplaceOrderByEffectiveFromDesc(UUID tenantId, String marketplace);

    List<FeeRuleRecord> findByTenantIdOrderByMarketplaceAscListingTypeAscEffectiveFromDesc(UUID tenantId);

    @Query("""
        SELECT r FROM FeeRuleRecord r
        WHERE r.tenantId = :tenantId
          AND r.marketplace = :marketplace
          AND r.listingType = :listingType
          AND (:categoryId IS NULL AND r.categoryId IS NULL OR r.categoryId = :categoryId)
          AND r.feeType = :feeType
          AND r.effectiveTo IS NULL
          AND r.isActive = true
    """)
    Optional<FeeRuleRecord> findCurrentOpenRule(
            @Param("tenantId") UUID tenantId,
            @Param("marketplace") String marketplace,
            @Param("listingType") String listingType,
            @Param("categoryId") String categoryId,
            @Param("feeType") FeeType feeType
    );

    @Query("""
        SELECT r FROM FeeRuleRecord r
        WHERE r.tenantId = :tenantId
          AND r.marketplace = :marketplace
          AND (r.listingType = :listingType OR r.listingType = 'ALL' OR r.listingType = 'DEFAULT')
          AND r.effectiveFrom <= :refDate
          AND (r.effectiveTo IS NULL OR r.effectiveTo > :refDate)
          AND r.isActive = true
        ORDER BY r.version DESC
    """)
    List<FeeRuleRecord> findCandidateRules(
            @Param("tenantId") UUID tenantId,
            @Param("marketplace") String marketplace,
            @Param("listingType") String listingType,
            @Param("refDate") Instant refDate
    );
}
