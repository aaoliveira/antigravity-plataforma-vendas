package br.com.sellerintelligence.sync.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListingRepository extends JpaRepository<Listing, UUID> {
    Optional<Listing> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<Listing> findByTenantIdAndMarketplaceAndExternalListingId(UUID tenantId, Marketplace marketplace, String externalListingId);
    List<Listing> findByTenantId(UUID tenantId);
    List<Listing> findByTenantIdAndStoreId(UUID tenantId, UUID storeId);
    long countByTenantId(UUID tenantId);
}
