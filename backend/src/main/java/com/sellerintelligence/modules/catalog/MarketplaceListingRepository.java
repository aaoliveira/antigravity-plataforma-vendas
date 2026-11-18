package com.sellerintelligence.modules.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketplaceListingRepository extends JpaRepository<MarketplaceListing, UUID> {

    Optional<MarketplaceListing> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<MarketplaceListing> findByTenantIdAndMarketplaceAndExternalListingId(
            UUID tenantId,
            String marketplace,
            String externalListingId
    );

    List<MarketplaceListing> findAllByTenantIdAndSellerAccountId(UUID tenantId, UUID sellerAccountId);

    List<MarketplaceListing> findAllByTenantIdAndProductId(UUID tenantId, UUID productId);

    List<MarketplaceListing> findAllByTenantId(UUID tenantId);

    long countByTenantId(UUID tenantId);
}
