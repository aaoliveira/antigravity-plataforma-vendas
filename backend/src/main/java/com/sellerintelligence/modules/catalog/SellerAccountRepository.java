package com.sellerintelligence.modules.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerAccountRepository extends JpaRepository<SellerAccount, UUID> {

    Optional<SellerAccount> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<SellerAccount> findByTenantIdAndMarketplaceAndExternalAccountId(
            UUID tenantId,
            String marketplace,
            String externalAccountId
    );

    List<SellerAccount> findAllByTenantId(UUID tenantId);

    List<SellerAccount> findAllByTenantIdAndMarketplace(UUID tenantId, String marketplace);
}
