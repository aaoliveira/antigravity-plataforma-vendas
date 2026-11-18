package com.sellerintelligence.modules.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ListingSyncCheckpointRepository extends JpaRepository<ListingSyncCheckpoint, UUID> {

    Optional<ListingSyncCheckpoint> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<ListingSyncCheckpoint> findByTenantIdAndSellerAccountIdAndMarketplace(
            UUID tenantId,
            UUID sellerAccountId,
            String marketplace
    );

    List<ListingSyncCheckpoint> findAllByTenantId(UUID tenantId);
}
