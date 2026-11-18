package com.sellerintelligence.modules.marketintelligence;

import com.sellerintelligence.modules.marketintelligence.model.MarketObservationSourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketObservationRepository extends JpaRepository<MarketObservation, UUID> {

    Optional<MarketObservation> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<MarketObservation> findByTenantIdAndMarketplaceAndObservedListingId(
            UUID tenantId,
            String marketplace,
            String observedListingId
    );

    Page<MarketObservation> findAllByTenantId(UUID tenantId, Pageable pageable);

    @Query("""
        SELECT o FROM MarketObservation o
        WHERE o.tenantId = :tenantId
          AND (:marketplace IS NULL OR o.marketplace = :marketplace)
          AND (:sourceType IS NULL OR o.sourceType = :sourceType)
          AND (:gtin IS NULL OR o.gtin = :gtin)
          AND (:startDate IS NULL OR o.collectedAt >= :startDate)
          AND (:endDate IS NULL OR o.collectedAt <= :endDate)
          AND (:search IS NULL OR LOWER(o.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(o.observedListingId) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<MarketObservation> findWithFilters(
            @Param("tenantId") UUID tenantId,
            @Param("marketplace") String marketplace,
            @Param("sourceType") MarketObservationSourceType sourceType,
            @Param("gtin") String gtin,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("search") String search,
            Pageable pageable
    );

    long countByTenantId(UUID tenantId);
}
