package br.com.sellerintelligence.sync.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    Optional<WebhookEvent> findByMarketplaceAndExternalEventId(Marketplace marketplace, String externalEventId);
    boolean existsByMarketplaceAndExternalEventId(Marketplace marketplace, String externalEventId);
}
