package br.com.sellerintelligence.reconciliation.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReconciliationSettlementRepository extends JpaRepository<ReconciliationSettlement, UUID> {
    List<ReconciliationSettlement> findByTenantIdOrderBySettlementDateDesc(UUID tenantId);
    Optional<ReconciliationSettlement> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<ReconciliationSettlement> findByTenantIdAndMarketplaceAndExternalSettlementId(
            UUID tenantId, String marketplace, String externalSettlementId
    );
    List<ReconciliationSettlement> findByTenantIdAndSettlementDateBetween(
            UUID tenantId, OffsetDateTime startDate, OffsetDateTime endDate
    );
}
