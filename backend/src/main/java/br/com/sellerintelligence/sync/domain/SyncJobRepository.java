package br.com.sellerintelligence.sync.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SyncJobRepository extends JpaRepository<SyncJob, UUID> {
    List<SyncJob> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<SyncJob> findByTenantIdAndStoreIdOrderByCreatedAtDesc(UUID tenantId, UUID storeId);
    Optional<SyncJob> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<SyncJob> findTopByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
