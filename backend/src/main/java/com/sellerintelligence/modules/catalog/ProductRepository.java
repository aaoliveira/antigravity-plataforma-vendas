package com.sellerintelligence.modules.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Product> findByTenantIdAndCanonicalSku(UUID tenantId, String canonicalSku);

    List<Product> findAllByTenantId(UUID tenantId);

    long countByTenantId(UUID tenantId);
}
