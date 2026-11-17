package br.com.sellerintelligence.connector.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketplaceCredentialRepository extends JpaRepository<MarketplaceCredential, UUID> {

    @Query("SELECT c FROM MarketplaceCredential c JOIN FETCH c.store WHERE c.store.id = :storeId")
    Optional<MarketplaceCredential> findByStoreIdWithStore(@Param("storeId") UUID storeId);

    Optional<MarketplaceCredential> findByStoreId(UUID storeId);

    List<MarketplaceCredential> findByTenantId(UUID tenantId);

    void deleteByStoreId(UUID storeId);
}
