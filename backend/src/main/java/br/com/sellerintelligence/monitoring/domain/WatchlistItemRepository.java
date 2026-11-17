package br.com.sellerintelligence.monitoring.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, UUID> {
    List<WatchlistItem> findByWatchlistIdAndTenantIdOrderByCreatedAtDesc(UUID watchlistId, UUID tenantId);
    List<WatchlistItem> findByTenantIdAndStatus(UUID tenantId, WatchlistItemStatus status);
    Optional<WatchlistItem> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByWatchlistIdAndMarketplaceAndItemTypeAndExternalId(UUID watchlistId, Marketplace marketplace, WatchlistItemType itemType, String externalId);
    Optional<WatchlistItem> findByWatchlistIdAndMarketplaceAndItemTypeAndExternalId(UUID watchlistId, Marketplace marketplace, WatchlistItemType itemType, String externalId);
}
