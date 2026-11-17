package br.com.sellerintelligence.compliance.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MarketplacePolicyChangeRepository extends JpaRepository<MarketplacePolicyChange, UUID> {

    List<MarketplacePolicyChange> findByMarketplaceOrderByDetectedAtDesc(Marketplace marketplace);

    List<MarketplacePolicyChange> findByPolicyIdOrderByDetectedAtDesc(UUID policyId);

    List<MarketplacePolicyChange> findByStatusOrderByDetectedAtDesc(ChangeAnalysisStatus status);

    List<MarketplacePolicyChange> findTop20ByOrderByDetectedAtDesc();
}
