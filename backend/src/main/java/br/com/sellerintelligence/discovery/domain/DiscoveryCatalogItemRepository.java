package br.com.sellerintelligence.discovery.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiscoveryCatalogItemRepository extends JpaRepository<DiscoveryCatalogItem, UUID> {

    List<DiscoveryCatalogItem> findByCatalogIdAndTenantId(UUID catalogId, UUID tenantId);

    List<DiscoveryCatalogItem> findByTenantIdAndReviewStatus(UUID tenantId, ItemReviewStatus reviewStatus);

    Optional<DiscoveryCatalogItem> findByIdAndTenantId(UUID id, UUID tenantId);

    long countByCatalogIdAndReviewStatus(UUID catalogId, ItemReviewStatus reviewStatus);
}
