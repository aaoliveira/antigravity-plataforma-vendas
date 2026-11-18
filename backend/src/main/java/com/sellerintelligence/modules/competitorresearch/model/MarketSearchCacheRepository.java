package com.sellerintelligence.modules.competitorresearch.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketSearchCacheRepository extends JpaRepository<MarketSearchCacheRecord, UUID> {

    Optional<MarketSearchCacheRecord> findByTenantIdAndCacheKey(UUID tenantId, String cacheKey);

    @Modifying
    @Query("DELETE FROM MarketSearchCacheRecord c WHERE c.expiresAt < :now")
    void deleteExpired(@Param("now") Instant now);

    long countByTenantId(UUID tenantId);
}
