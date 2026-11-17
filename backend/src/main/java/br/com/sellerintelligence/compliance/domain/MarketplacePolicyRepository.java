package br.com.sellerintelligence.compliance.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketplacePolicyRepository extends JpaRepository<MarketplacePolicy, UUID> {

    List<MarketplacePolicy> findByMarketplace(Marketplace marketplace);

    List<MarketplacePolicy> findByMarketplaceAndStatus(Marketplace marketplace, PolicyStatus status);

    List<MarketplacePolicy> findByStatus(PolicyStatus status);

    Optional<MarketplacePolicy> findByMarketplaceAndPolicyName(Marketplace marketplace, String policyName);

    Optional<MarketplacePolicy> findByOfficialUrl(String officialUrl);

    long countByStatus(PolicyStatus status);
}
