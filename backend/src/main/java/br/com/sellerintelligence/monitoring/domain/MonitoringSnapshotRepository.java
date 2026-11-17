package br.com.sellerintelligence.monitoring.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonitoringSnapshotRepository extends JpaRepository<MonitoringSnapshot, UUID> {
    List<MonitoringSnapshot> findByItemIdAndTenantIdOrderByCollectedAtAsc(UUID itemId, UUID tenantId);
    List<MonitoringSnapshot> findByItemIdAndTenantIdOrderByCollectedAtDesc(UUID itemId, UUID tenantId);
    Optional<MonitoringSnapshot> findFirstByItemIdAndTenantIdOrderByCollectedAtDesc(UUID itemId, UUID tenantId);
    List<MonitoringSnapshot> findByTenantIdOrderByCollectedAtDesc(UUID tenantId);
    long countByItemIdAndTenantId(UUID itemId, UUID tenantId);
    long deleteByCollectedAtBefore(Instant cutoff);
}
