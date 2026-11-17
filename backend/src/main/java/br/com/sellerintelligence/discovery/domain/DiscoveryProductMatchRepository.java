package br.com.sellerintelligence.discovery.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiscoveryProductMatchRepository extends JpaRepository<DiscoveryProductMatch, UUID> {

    List<DiscoveryProductMatch> findByCatalogItemIdAndTenantId(UUID catalogItemId, UUID tenantId);

    List<DiscoveryProductMatch> findByTenantIdAndStatus(UUID tenantId, MatchStatus status);

    Optional<DiscoveryProductMatch> findByIdAndTenantId(UUID id, UUID tenantId);
}
