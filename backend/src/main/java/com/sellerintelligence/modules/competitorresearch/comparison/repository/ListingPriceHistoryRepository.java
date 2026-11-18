package com.sellerintelligence.modules.competitorresearch.comparison.repository;

import com.sellerintelligence.modules.competitorresearch.comparison.entity.ListingPriceHistoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ListingPriceHistoryRepository extends JpaRepository<ListingPriceHistoryRecord, UUID> {

    Optional<ListingPriceHistoryRecord> findByIdAndTenantId(UUID id, UUID tenantId);

    List<ListingPriceHistoryRecord> findAllByTenantIdAndMarketplaceAndExternalListingIdOrderByObservedAtAsc(
            UUID tenantId,
            String marketplace,
            String externalListingId
    );

    @Query("""
        SELECT h FROM ListingPriceHistoryRecord h
        WHERE h.tenantId = :tenantId
          AND h.marketplace = :marketplace
          AND h.externalListingId = :externalListingId
          AND (:startDate IS NULL OR h.observedAt >= :startDate)
          AND (:endDate IS NULL OR h.observedAt <= :endDate)
        ORDER BY h.observedAt ASC
    """)
    List<ListingPriceHistoryRecord> findHistoryInWindow(
            @Param("tenantId") UUID tenantId,
            @Param("marketplace") String marketplace,
            @Param("externalListingId") String externalListingId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    long countByTenantIdAndMarketplaceAndExternalListingId(
            UUID tenantId,
            String marketplace,
            String externalListingId
    );
}
