package br.com.sellerintelligence.action.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActionAlertRepository extends JpaRepository<ActionAlert, UUID> {

    List<ActionAlert> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<ActionAlert> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, ActionAlertStatus status);

    List<ActionAlert> findByTenantIdAndDomainTypeOrderByCreatedAtDesc(UUID tenantId, ActionAlertDomain domainType);

    Optional<ActionAlert> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<ActionAlert> findByTenantIdAndDedupKey(UUID tenantId, String dedupKey);

    long countByTenantIdAndStatus(UUID tenantId, ActionAlertStatus status);
}
