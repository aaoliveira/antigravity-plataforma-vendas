package com.sellerintelligence.modules.listinganalytics.repository;

import com.sellerintelligence.modules.listinganalytics.entity.AccountMetricRecord;
import com.sellerintelligence.modules.listinganalytics.model.MetricAvailabilityStatus;
import com.sellerintelligence.modules.listinganalytics.model.MetricGranularity;
import com.sellerintelligence.modules.listinganalytics.model.MetricScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountMetricRepository extends JpaRepository<AccountMetricRecord, UUID> {

    Optional<AccountMetricRecord> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<AccountMetricRecord> findByTenantIdAndSellerAccountIdAndMarketplaceAndTargetIdentifierAndMetricKeyAndGranularityAndWindowStartAndWindowEnd(
            UUID tenantId,
            UUID sellerAccountId,
            String marketplace,
            String targetIdentifier,
            String metricKey,
            MetricGranularity granularity,
            Instant windowStart,
            Instant windowEnd
    );

    List<AccountMetricRecord> findAllByTenantIdAndSellerAccountId(UUID tenantId, UUID sellerAccountId);

    @Query("SELECT m FROM AccountMetricRecord m WHERE m.tenantId = :tenantId " +
            "AND (:sellerAccountId IS NULL OR m.sellerAccountId = :sellerAccountId) " +
            "AND (:marketplace IS NULL OR :marketplace = '' OR m.marketplace = :marketplace) " +
            "AND (:externalListingId IS NULL OR :externalListingId = '' OR m.externalListingId = :externalListingId) " +
            "AND (:metricScope IS NULL OR m.metricScope = :metricScope) " +
            "AND (:metricKey IS NULL OR :metricKey = '' OR m.metricKey = :metricKey) " +
            "AND (:status IS NULL OR m.availabilityStatus = :status) " +
            "AND (:startDate IS NULL OR m.windowEnd >= :startDate) " +
            "AND (:endDate IS NULL OR m.windowStart <= :endDate)")
    Page<AccountMetricRecord> searchMetrics(
            @Param("tenantId") UUID tenantId,
            @Param("sellerAccountId") UUID sellerAccountId,
            @Param("marketplace") String marketplace,
            @Param("externalListingId") String externalListingId,
            @Param("metricScope") MetricScope metricScope,
            @Param("metricKey") String metricKey,
            @Param("status") MetricAvailabilityStatus status,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );

    @Query("SELECT m FROM AccountMetricRecord m WHERE m.tenantId = :tenantId " +
            "AND (:sellerAccountId IS NULL OR m.sellerAccountId = :sellerAccountId) " +
            "AND (:marketplace IS NULL OR :marketplace = '' OR m.marketplace = :marketplace) " +
            "AND (:externalListingId IS NULL OR :externalListingId = '' OR m.externalListingId = :externalListingId) " +
            "AND (:startDate IS NULL OR m.windowEnd >= :startDate) " +
            "AND (:endDate IS NULL OR m.windowStart <= :endDate)")
    List<AccountMetricRecord> findForSummary(
            @Param("tenantId") UUID tenantId,
            @Param("sellerAccountId") UUID sellerAccountId,
            @Param("marketplace") String marketplace,
            @Param("externalListingId") String externalListingId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    long countByTenantId(UUID tenantId);
}
