package br.com.sellerintelligence.market.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedSearchRepository extends JpaRepository<SavedSearch, UUID> {
    List<SavedSearch> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<SavedSearch> findByIdAndTenantId(UUID id, UUID tenantId);
}
