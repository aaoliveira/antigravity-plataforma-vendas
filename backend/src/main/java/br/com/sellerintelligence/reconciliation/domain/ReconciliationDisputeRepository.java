package br.com.sellerintelligence.reconciliation.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReconciliationDisputeRepository extends JpaRepository<ReconciliationDispute, UUID> {
    List<ReconciliationDispute> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<ReconciliationDispute> findByIdAndTenantId(UUID id, UUID tenantId);
    List<ReconciliationDispute> findByTenantIdAndStatus(UUID tenantId, DisputeStatus status);
}
