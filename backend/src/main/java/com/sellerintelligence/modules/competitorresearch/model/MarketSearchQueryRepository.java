package com.sellerintelligence.modules.competitorresearch.model;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MarketSearchQueryRepository extends JpaRepository<MarketSearchQueryRecord, UUID> {

    Page<MarketSearchQueryRecord> findAllByTenantId(UUID tenantId, Pageable pageable);

    long countByTenantId(UUID tenantId);
}
