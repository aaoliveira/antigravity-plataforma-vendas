package com.sellerintelligence.modules.competitorresearch.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketSearchSourcePolicyRepository extends JpaRepository<MarketSearchSourcePolicy, String> {

    List<MarketSearchSourcePolicy> findAllByIsActiveTrue();

    Optional<MarketSearchSourcePolicy> findByMarketplaceAndIsActiveTrue(String marketplace);

    Optional<MarketSearchSourcePolicy> findByIdAndIsActiveTrue(String id);
}
