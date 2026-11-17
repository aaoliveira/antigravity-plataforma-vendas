package br.com.sellerintelligence.ai.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ListingVersionRepository extends JpaRepository<ListingVersion, UUID> {

    @Query("SELECT v FROM ListingVersion v WHERE v.tenantId = :tenantId AND v.listingId = :listingId ORDER BY v.versionNumber DESC")
    List<ListingVersion> findByTenantIdAndListingIdOrderByVersionDesc(
            @Param("tenantId") UUID tenantId,
            @Param("listingId") UUID listingId
    );

    @Query("SELECT COALESCE(MAX(v.versionNumber), 0) FROM ListingVersion v WHERE v.tenantId = :tenantId AND v.listingId = :listingId")
    Integer findMaxVersionNumber(
            @Param("tenantId") UUID tenantId,
            @Param("listingId") UUID listingId
    );

    @Query("SELECT v FROM ListingVersion v WHERE v.tenantId = :tenantId AND v.id = :id")
    Optional<ListingVersion> findByTenantIdAndId(
            @Param("tenantId") UUID tenantId,
            @Param("id") UUID id
    );
}
