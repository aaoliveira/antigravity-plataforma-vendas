package br.com.sellerintelligence.sync.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByTenantIdAndMarketplaceAndExternalOrderId(UUID tenantId, Marketplace marketplace, String externalOrderId);
    List<Order> findByTenantIdOrderByOrderDateDesc(UUID tenantId);
    List<Order> findByTenantIdAndStoreIdOrderByOrderDateDesc(UUID tenantId, UUID storeId);
    long countByTenantId(UUID tenantId);
}
