package br.com.sellerintelligence.monitoring.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonitoringAlertRuleRepository extends JpaRepository<MonitoringAlertRule, UUID> {
    List<MonitoringAlertRule> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<MonitoringAlertRule> findByTenantIdAndStatus(UUID tenantId, String status);
    Optional<MonitoringAlertRule> findByIdAndTenantId(UUID id, UUID tenantId);
}
