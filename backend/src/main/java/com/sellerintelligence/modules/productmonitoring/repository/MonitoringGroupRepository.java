package com.sellerintelligence.modules.productmonitoring.repository;

import com.sellerintelligence.modules.productmonitoring.entity.MonitoringGroupRecord;
import com.sellerintelligence.modules.productmonitoring.model.MonitoringGroupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonitoringGroupRepository extends JpaRepository<MonitoringGroupRecord, UUID> {

    Optional<MonitoringGroupRecord> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndNameIgnoreCaseAndStatusNot(UUID tenantId, String name, MonitoringGroupStatus status);

    Page<MonitoringGroupRecord> findAllByTenantIdAndStatusNot(UUID tenantId, MonitoringGroupStatus status, Pageable pageable);

    @Query("SELECT g FROM MonitoringGroupRecord g WHERE g.tenantId = :tenantId AND g.status <> 'DELETED' AND LOWER(g.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<MonitoringGroupRecord> searchByTenantIdAndName(
            @Param("tenantId") UUID tenantId,
            @Param("search") String search,
            Pageable pageable
    );

    List<MonitoringGroupRecord> findAllByTenantIdAndStatus(UUID tenantId, MonitoringGroupStatus status);
}
