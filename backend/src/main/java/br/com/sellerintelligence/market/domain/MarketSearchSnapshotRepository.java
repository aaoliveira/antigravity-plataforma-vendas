package br.com.sellerintelligence.market.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarketSearchSnapshotRepository extends JpaRepository<MarketSearchSnapshot, UUID> {
    List<MarketSearchSnapshot> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<MarketSearchSnapshot> findByIdAndTenantId(UUID id, UUID tenantId);
}
