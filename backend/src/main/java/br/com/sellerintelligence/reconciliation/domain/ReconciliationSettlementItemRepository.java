package br.com.sellerintelligence.reconciliation.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReconciliationSettlementItemRepository extends JpaRepository<ReconciliationSettlementItem, UUID> {
    List<ReconciliationSettlementItem> findBySettlementIdAndTenantId(UUID settlementId, UUID tenantId);
    List<ReconciliationSettlementItem> findByTenantIdAndReconciliationStatus(UUID tenantId, ReconciliationItemStatus status);
    Optional<ReconciliationSettlementItem> findByIdAndTenantId(UUID id, UUID tenantId);
    List<ReconciliationSettlementItem> findByTenantIdAndExternalOrderId(UUID tenantId, String externalOrderId);
}
