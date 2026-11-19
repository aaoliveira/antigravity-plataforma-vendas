package com.sellerintelligence.modules.financialanalytics.repository;

import com.sellerintelligence.modules.financialanalytics.entity.OrderFinancialFactRecord;
import com.sellerintelligence.modules.financialanalytics.model.ReconciliationStatus;
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
public interface OrderFinancialFactRepository extends JpaRepository<OrderFinancialFactRecord, UUID> {

    Optional<OrderFinancialFactRecord> findByTenantIdAndOrderId(UUID tenantId, UUID orderId);

    Optional<OrderFinancialFactRecord> findByTenantIdAndExternalOrderId(UUID tenantId, String externalOrderId);

    @Query("""
        SELECT f FROM OrderFinancialFactRecord f
        WHERE f.tenantId = :tenantId
          AND (:companyId IS NULL OR f.companyId = :companyId)
          AND (:marketplace IS NULL OR f.marketplace = :marketplace)
          AND (:status IS NULL OR f.reconciliationStatus = :status)
          AND (:startDate IS NULL OR f.orderDate >= :startDate)
          AND (:endDate IS NULL OR f.orderDate <= :endDate)
        ORDER BY f.orderDate DESC
    """)
    Page<OrderFinancialFactRecord> searchFacts(
            @Param("tenantId") UUID tenantId,
            @Param("companyId") UUID companyId,
            @Param("marketplace") String marketplace,
            @Param("status") ReconciliationStatus status,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );

    @Query("""
        SELECT f FROM OrderFinancialFactRecord f
        WHERE f.tenantId = :tenantId
          AND (:companyId IS NULL OR f.companyId = :companyId)
          AND (:marketplace IS NULL OR f.marketplace = :marketplace)
          AND (:startDate IS NULL OR f.orderDate >= :startDate)
          AND (:endDate IS NULL OR f.orderDate <= :endDate)
    """)
    List<OrderFinancialFactRecord> findForSummary(
            @Param("tenantId") UUID tenantId,
            @Param("companyId") UUID companyId,
            @Param("marketplace") String marketplace,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );
}
