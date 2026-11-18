package com.sellerintelligence.modules.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExternalIdentifierMappingRepository extends JpaRepository<ExternalIdentifierMapping, UUID> {

    Optional<ExternalIdentifierMapping> findByTenantIdAndMarketplaceAndExternalIdTypeAndExternalIdValue(
            UUID tenantId,
            String marketplace,
            String externalIdType,
            String externalIdValue
    );

    List<ExternalIdentifierMapping> findAllByTenantIdAndInternalEntityId(UUID tenantId, UUID internalEntityId);

    List<ExternalIdentifierMapping> findAllByTenantId(UUID tenantId);
}
