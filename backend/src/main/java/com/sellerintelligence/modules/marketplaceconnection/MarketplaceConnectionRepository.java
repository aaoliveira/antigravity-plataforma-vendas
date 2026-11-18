package com.sellerintelligence.modules.marketplaceconnection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório tenant-aware com filtros explícitos no nível da aplicação.
 * Trabalha em conjunto com a RLS do PostgreSQL (Defesa em Profundidade).
 */
@Repository
public interface MarketplaceConnectionRepository extends JpaRepository<MarketplaceConnection, UUID> {

    List<MarketplaceConnection> findAllByTenantId(UUID tenantId);

    Optional<MarketplaceConnection> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<MarketplaceConnection> findByTenantIdAndMarketplaceAndSellerAccountId(
            UUID tenantId,
            String marketplace,
            String sellerAccountId
    );

    List<MarketplaceConnection> findAllByTenantIdAndStatus(UUID tenantId, String status);

    boolean existsByTenantIdAndId(UUID tenantId, UUID id);

    void deleteByTenantIdAndId(UUID tenantId, UUID id);
}
