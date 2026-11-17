package br.com.sellerintelligence.reconciliation.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReconciliationAuditLogRepository extends JpaRepository<ReconciliationAuditLog, UUID> {
    List<ReconciliationAuditLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<ReconciliationAuditLog> findBySettlementItemIdOrderByCreatedAtDesc(UUID settlementItemId);
}
