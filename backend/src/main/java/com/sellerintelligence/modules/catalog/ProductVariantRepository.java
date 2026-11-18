package com.sellerintelligence.modules.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    Optional<ProductVariant> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<ProductVariant> findByTenantIdAndVariantSku(UUID tenantId, String variantSku);

    Optional<ProductVariant> findByTenantIdAndGtinBarcode(UUID tenantId, String gtinBarcode);

    List<ProductVariant> findAllByTenantIdAndProductId(UUID tenantId, UUID productId);

    long countByTenantId(UUID tenantId);
}
