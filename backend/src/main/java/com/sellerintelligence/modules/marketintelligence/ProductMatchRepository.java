package com.sellerintelligence.modules.marketintelligence;

import com.sellerintelligence.modules.marketintelligence.model.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductMatchRepository extends JpaRepository<ProductMatch, UUID> {

    Optional<ProductMatch> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<ProductMatch> findByTenantIdAndCanonicalProductIdAndObservationId(
            UUID tenantId,
            UUID canonicalProductId,
            UUID observationId
    );

    List<ProductMatch> findAllByTenantIdAndObservationId(UUID tenantId, UUID observationId);

    List<ProductMatch> findAllByTenantIdAndCanonicalProductId(UUID tenantId, UUID canonicalProductId);

    Page<ProductMatch> findAllByTenantId(UUID tenantId, Pageable pageable);

    Page<ProductMatch> findAllByTenantIdAndMatchStatus(UUID tenantId, MatchStatus matchStatus, Pageable pageable);

    long countByTenantId(UUID tenantId);
}
