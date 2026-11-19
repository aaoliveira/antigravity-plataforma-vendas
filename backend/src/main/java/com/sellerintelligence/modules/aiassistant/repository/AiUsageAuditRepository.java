package com.sellerintelligence.modules.aiassistant.repository;

import com.sellerintelligence.modules.aiassistant.entity.AiUsageAuditRecord;
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
public interface AiUsageAuditRepository extends JpaRepository<AiUsageAuditRecord, UUID> {

    Optional<AiUsageAuditRecord> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("SELECT a FROM AiUsageAuditRecord a WHERE a.tenantId = :tenantId " +
            "AND (:promptKey IS NULL OR :promptKey = '' OR a.promptKey = :promptKey) " +
            "AND (:provider IS NULL OR :provider = '' OR a.provider = :provider) " +
            "AND (:status IS NULL OR :status = '' OR a.status = :status) " +
            "AND (:startDate IS NULL OR a.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR a.createdAt <= :endDate) " +
            "ORDER BY a.createdAt DESC")
    Page<AiUsageAuditRecord> searchAudits(
            @Param("tenantId") UUID tenantId,
            @Param("promptKey") String promptKey,
            @Param("provider") String provider,
            @Param("status") String status,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );

    long countByTenantId(UUID tenantId);
}
