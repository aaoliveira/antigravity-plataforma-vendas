package br.com.sellerintelligence.compliance.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeatureEnforcementRepository extends JpaRepository<FeatureEnforcementRecord, UUID> {

    Optional<FeatureEnforcementRecord> findByMarketplaceAndFeatureName(Marketplace marketplace, String featureName);

    List<FeatureEnforcementRecord> findByMarketplace(Marketplace marketplace);

    List<FeatureEnforcementRecord> findByStatus(PolicyStatus status);

    long countByStatus(PolicyStatus status);
}
