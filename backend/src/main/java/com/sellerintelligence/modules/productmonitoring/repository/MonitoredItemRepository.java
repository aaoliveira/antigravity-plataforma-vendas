package com.sellerintelligence.modules.productmonitoring.repository;

import com.sellerintelligence.modules.productmonitoring.entity.MonitoredItemRecord;
import com.sellerintelligence.modules.productmonitoring.model.MonitoredItemStatus;
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
public interface MonitoredItemRepository extends JpaRepository<MonitoredItemRecord, UUID> {

    Optional<MonitoredItemRecord> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndGroupIdAndMarketplaceAndExternalListingIdAndStatusNot(
            UUID tenantId, UUID groupId, String marketplace, String externalListingId, MonitoredItemStatus status
    );

    Page<MonitoredItemRecord> findAllByTenantIdAndGroupIdAndStatusNot(
            UUID tenantId, UUID groupId, MonitoredItemStatus status, Pageable pageable
    );

    List<MonitoredItemRecord> findAllByTenantIdAndGroupIdAndStatusNot(
            UUID tenantId, UUID groupId, MonitoredItemStatus status
    );

    int countByTenantIdAndGroupIdAndStatusNot(UUID tenantId, UUID groupId, MonitoredItemStatus status);

    @Query("SELECT i FROM MonitoredItemRecord i WHERE i.status = 'ACTIVE' AND i.nextRunAt <= :now")
    List<MonitoredItemRecord> findDueItemsForExecution(@Param("now") Instant now, Pageable pageable);

    List<MonitoredItemRecord> findAllByTenantIdAndStatus(UUID tenantId, MonitoredItemStatus status);
}
