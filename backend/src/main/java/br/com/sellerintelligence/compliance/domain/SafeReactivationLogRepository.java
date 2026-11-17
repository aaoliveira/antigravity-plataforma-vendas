package br.com.sellerintelligence.compliance.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SafeReactivationLogRepository extends JpaRepository<SafeReactivationLog, UUID> {

    List<SafeReactivationLog> findByMarketplaceOrderByApprovedAtDesc(Marketplace marketplace);

    List<SafeReactivationLog> findTop20ByOrderByApprovedAtDesc();
}
