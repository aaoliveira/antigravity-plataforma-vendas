package br.com.sellerintelligence.compliance.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketplaceApiLifecycleRepository extends JpaRepository<MarketplaceApiLifecycle, UUID> {

    List<MarketplaceApiLifecycle> findByMarketplace(Marketplace marketplace);

    List<MarketplaceApiLifecycle> findByStatus(ApiLifecycleStatus status);

    Optional<MarketplaceApiLifecycle> findByMarketplaceAndApiNameAndVersion(Marketplace marketplace, String apiName, String version);
}
