package com.sellerintelligence.modules.orderingestion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderSyncCheckpointRepository extends JpaRepository<OrderSyncCheckpoint, UUID> {

    Optional<OrderSyncCheckpoint> findByTenantIdAndSellerAccountIdAndMarketplace(
            UUID tenantId,
            UUID sellerAccountId,
            String marketplace
    );

    List<OrderSyncCheckpoint> findAllByTenantId(UUID tenantId);
}
