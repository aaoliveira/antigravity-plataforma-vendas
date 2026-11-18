package com.sellerintelligence.modules.productmonitoring.repository;

import com.sellerintelligence.modules.productmonitoring.entity.MonitoredItemSnapshotRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MonitoredItemSnapshotRepository extends JpaRepository<MonitoredItemSnapshotRecord, UUID> {

    List<MonitoredItemSnapshotRecord> findAllByTenantIdAndItemIdOrderByObservedAtDesc(UUID tenantId, UUID itemId);

    Page<MonitoredItemSnapshotRecord> findAllByTenantIdAndItemIdOrderByObservedAtDesc(UUID tenantId, UUID itemId, Pageable pageable);

    @Query("SELECT s FROM MonitoredItemSnapshotRecord s WHERE s.tenantId = :tenantId AND s.itemId = :itemId AND s.observedAt >= :afterInstant ORDER BY s.observedAt DESC")
    List<MonitoredItemSnapshotRecord> findSnapshotsSinceInclusion(
            @Param("tenantId") UUID tenantId,
            @Param("itemId") UUID itemId,
            @Param("afterInstant") Instant afterInstant
    );

    int countByTenantIdAndItemId(UUID tenantId, UUID itemId);

    @Modifying
    @Query("DELETE FROM MonitoredItemSnapshotRecord s WHERE s.tenantId = :tenantId AND s.observedAt < :cutoff")
    int deleteSnapshotsOlderThan(@Param("tenantId") UUID tenantId, @Param("cutoff") Instant cutoff);
}
