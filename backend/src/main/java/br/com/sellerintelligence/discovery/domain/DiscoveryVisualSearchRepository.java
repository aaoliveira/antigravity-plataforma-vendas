package br.com.sellerintelligence.discovery.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DiscoveryVisualSearchRepository extends JpaRepository<DiscoveryVisualSearch, UUID> {

    List<DiscoveryVisualSearch> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
