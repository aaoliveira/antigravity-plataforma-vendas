package com.sellerintelligence.modules.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT l FROM MarketplaceListing l WHERE l.tenantId = :tenantId " +
           "AND (:marketplace IS NULL OR :marketplace = '' OR l.marketplace = :marketplace) " +
           "AND (:sellerAccountId IS NULL OR l.sellerAccountId = :sellerAccountId) " +
           "AND (:status IS NULL OR :status = '' OR l.status = :status) " +
           "AND (:search IS NULL OR :search = '' OR " +
           "     LOWER(l.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(l.externalListingId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(l.externalSku) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<MarketplaceListing> searchListings(
            @Param("tenantId") UUID tenantId,
            @Param("marketplace") String marketplace,
            @Param("sellerAccountId") UUID sellerAccountId,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable
    );
}

