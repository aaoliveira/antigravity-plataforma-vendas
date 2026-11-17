package br.com.sellerintelligence.monitoring.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, UUID> {
    List<Watchlist> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<Watchlist> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByIdAndTenantId(UUID id, UUID tenantId);
}
