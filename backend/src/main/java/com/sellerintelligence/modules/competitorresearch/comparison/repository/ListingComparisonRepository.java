package com.sellerintelligence.modules.competitorresearch.comparison.repository;

import com.sellerintelligence.modules.competitorresearch.comparison.entity.ListingComparisonRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ListingComparisonRepository extends JpaRepository<ListingComparisonRecord, UUID> {

    Optional<ListingComparisonRecord> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<ListingComparisonRecord> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    long countByTenantId(UUID tenantId);
}
