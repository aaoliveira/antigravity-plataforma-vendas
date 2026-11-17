package br.com.sellerintelligence.action.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActionTaskRepository extends JpaRepository<ActionTask, UUID> {

    List<ActionTask> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<ActionTask> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, TaskStatus status);

    List<ActionTask> findByTenantIdAndAssignedToUserIdOrderByCreatedAtDesc(UUID tenantId, UUID assignedToUserId);

    Optional<ActionTask> findByIdAndTenantId(UUID id, UUID tenantId);

    List<ActionTask> findByTenantIdAndAlertId(UUID tenantId, UUID alertId);
}
