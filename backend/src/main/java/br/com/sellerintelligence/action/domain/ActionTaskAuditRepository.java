package br.com.sellerintelligence.action.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActionTaskAuditRepository extends JpaRepository<ActionTaskAudit, UUID> {

    List<ActionTaskAudit> findByTaskIdOrderByCreatedAtAsc(UUID taskId);
}
