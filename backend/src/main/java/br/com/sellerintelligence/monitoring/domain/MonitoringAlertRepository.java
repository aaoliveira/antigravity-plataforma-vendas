package br.com.sellerintelligence.monitoring.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonitoringAlertRepository extends JpaRepository<MonitoringAlert, UUID> {
    List<MonitoringAlert> findByTenantIdOrderByTriggeredAtDesc(UUID tenantId);
    List<MonitoringAlert> findByTenantIdAndReadFalseOrderByTriggeredAtDesc(UUID tenantId);
    Optional<MonitoringAlert> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByTenantIdAndDedupKey(UUID tenantId, String dedupKey);
    Optional<MonitoringAlert> findByTenantIdAndDedupKey(UUID tenantId, String dedupKey);
}
