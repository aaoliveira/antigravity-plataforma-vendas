package com.sellerintelligence.modules.company.repository;

import com.sellerintelligence.modules.company.entity.CompanyRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<CompanyRecord, UUID> {

    Optional<CompanyRecord> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<CompanyRecord> findByTenantIdAndCnpj(UUID tenantId, String cnpj);

    boolean existsByTenantIdAndCnpj(UUID tenantId, String cnpj);

    List<CompanyRecord> findAllByTenantIdOrderByCorporateNameAsc(UUID tenantId);

    @Query("SELECT c FROM CompanyRecord c WHERE c.tenantId = :tenantId " +
           "AND (:activeOnly = false OR c.isActive = true) " +
           "AND (:search IS NULL OR LOWER(c.corporateName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.tradeName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR c.cnpj LIKE CONCAT('%', :search, '%')) " +
           "ORDER BY c.corporateName ASC")
    Page<CompanyRecord> findByTenantIdWithFilter(
            @Param("tenantId") UUID tenantId,
            @Param("search") String search,
            @Param("activeOnly") boolean activeOnly,
            Pageable pageable
    );
}
