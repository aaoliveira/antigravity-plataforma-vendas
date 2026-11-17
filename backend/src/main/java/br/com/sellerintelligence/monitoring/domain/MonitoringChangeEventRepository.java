package br.com.sellerintelligence.monitoring.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonitoringChangeEventRepository extends JpaRepository<MonitoringChangeEvent, UUID> {
    List<MonitoringChangeEvent> findByItemIdAndTenantIdOrderByDetectedAtDesc(UUID itemId, UUID tenantId);
    List<MonitoringChangeEvent> findByTenantIdOrderByDetectedAtDesc(UUID tenantId);
    boolean existsByTenantIdAndEventHash(UUID tenantId, String eventHash);
    Optional<MonitoringChangeEvent> findByTenantIdAndEventHash(UUID tenantId, String eventHash);
}
