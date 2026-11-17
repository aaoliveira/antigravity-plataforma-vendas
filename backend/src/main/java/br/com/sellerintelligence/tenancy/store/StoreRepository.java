package br.com.sellerintelligence.tenancy.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreRepository extends JpaRepository<Store, UUID> {

    @Query("SELECT s FROM Store s JOIN FETCH s.company WHERE s.tenantId = :tenantId")
    List<Store> findByTenantIdWithCompany(@Param("tenantId") UUID tenantId);

    @Query("SELECT s FROM Store s JOIN FETCH s.company WHERE s.tenantId = :tenantId AND s.company.id = :companyId")
    List<Store> findByTenantIdAndCompanyIdWithCompany(@Param("tenantId") UUID tenantId, @Param("companyId") UUID companyId);

    @Query("SELECT s FROM Store s JOIN FETCH s.company WHERE s.id = :id AND s.tenantId = :tenantId")
    Optional<Store> findByIdAndTenantIdWithCompany(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    List<Store> findByCompanyId(UUID companyId);

    long countByCompanyId(UUID companyId);

    Optional<Store> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Store> findByTenantId(UUID tenantId);

    Optional<Store> findByMarketplaceAndExternalAccountId(Marketplace marketplace, String externalAccountId);
}
