package br.com.sellerintelligence.market.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedMarketProductRepository extends JpaRepository<SavedMarketProduct, UUID> {
    List<SavedMarketProduct> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<SavedMarketProduct> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<SavedMarketProduct> findByTenantIdAndMarketplaceAndExternalItemId(UUID tenantId, Marketplace marketplace, String externalItemId);
    void deleteByTenantIdAndId(UUID tenantId, UUID id);
}
