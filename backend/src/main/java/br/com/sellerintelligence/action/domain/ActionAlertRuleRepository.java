package br.com.sellerintelligence.action.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActionAlertRuleRepository extends JpaRepository<ActionAlertRule, UUID> {

    List<ActionAlertRule> findByTenantId(UUID tenantId);

    List<ActionAlertRule> findByTenantIdAndActive(UUID tenantId, boolean active);

    Optional<ActionAlertRule> findByIdAndTenantId(UUID id, UUID tenantId);

    List<ActionAlertRule> findByTenantIdAndRuleType(UUID tenantId, AlertRuleType ruleType);
}
