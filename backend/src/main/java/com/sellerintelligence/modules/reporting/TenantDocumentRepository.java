package com.sellerintelligence.modules.reporting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório tenant-aware para gerenciamento de documentos e relatórios tenant-scoped.
 */
@Repository
public interface TenantDocumentRepository extends JpaRepository<TenantDocument, UUID> {

    List<TenantDocument> findAllByTenantId(UUID tenantId);

    Optional<TenantDocument> findByTenantIdAndId(UUID tenantId, UUID id);

    boolean existsByTenantIdAndId(UUID tenantId, UUID id);

    void deleteByTenantIdAndId(UUID tenantId, UUID id);
}
