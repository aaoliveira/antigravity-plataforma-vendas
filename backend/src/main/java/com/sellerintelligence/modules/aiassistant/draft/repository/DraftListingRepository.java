package com.sellerintelligence.modules.aiassistant.draft.repository;

import com.sellerintelligence.modules.aiassistant.draft.entity.DraftListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DraftListingRepository extends JpaRepository<DraftListing, UUID> {

    Optional<DraftListing> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<DraftListing> findByTenantId(UUID tenantId, Pageable pageable);

    Page<DraftListing> findByTenantIdAndTargetMarketplace(UUID tenantId, String targetMarketplace, Pageable pageable);

    Page<DraftListing> findByTenantIdAndStatus(UUID tenantId, String status, Pageable pageable);

    Page<DraftListing> findByTenantIdAndTargetMarketplaceAndStatus(
            UUID tenantId,
            String targetMarketplace,
            String status,
            Pageable pageable
    );
}
