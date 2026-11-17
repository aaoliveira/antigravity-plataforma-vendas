package br.com.sellerintelligence.discovery.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiscoveryCatalogRepository extends JpaRepository<DiscoveryCatalog, UUID> {

    List<DiscoveryCatalog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<DiscoveryCatalog> findByIdAndTenantId(UUID id, UUID tenantId);

    List<DiscoveryCatalog> findByTenantIdAndStatus(UUID tenantId, CatalogStatus status);
}
