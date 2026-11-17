package br.com.sellerintelligence.ai.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ListingQualityAuditRepository extends JpaRepository<ListingQualityAudit, UUID> {

    @Query("SELECT a FROM ListingQualityAudit a WHERE a.tenantId = :tenantId AND a.listingId = :listingId ORDER BY a.createdAt DESC LIMIT 1")
    Optional<ListingQualityAudit> findLatestByTenantIdAndListingId(
            @Param("tenantId") UUID tenantId,
            @Param("listingId") UUID listingId
    );
}
