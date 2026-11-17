package br.com.sellerintelligence.sync.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findByTenantIdAndStoreIdAndSku(UUID tenantId, UUID storeId, String sku);
    List<Product> findByTenantId(UUID tenantId);
    List<Product> findByTenantIdAndStoreId(UUID tenantId, UUID storeId);
    long countByTenantId(UUID tenantId);
}
