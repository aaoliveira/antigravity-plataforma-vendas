package com.sellerintelligence.modules.company.repository;

import com.sellerintelligence.modules.company.entity.CompanyAccountLinkRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyAccountLinkRepository extends JpaRepository<CompanyAccountLinkRecord, UUID> {

    Optional<CompanyAccountLinkRecord> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("SELECT l FROM CompanyAccountLinkRecord l " +
           "WHERE l.tenantId = :tenantId AND l.sellerAccountId = :sellerAccountId AND l.isActive = true")
    Optional<CompanyAccountLinkRecord> findActiveBySellerAccount(
            @Param("tenantId") UUID tenantId,
            @Param("sellerAccountId") UUID sellerAccountId
    );

    @Query("SELECT l FROM CompanyAccountLinkRecord l " +
           "WHERE l.tenantId = :tenantId AND l.companyId = :companyId AND l.isActive = true " +
           "ORDER BY l.createdAt DESC")
    List<CompanyAccountLinkRecord> findAllActiveByCompany(
            @Param("tenantId") UUID tenantId,
            @Param("companyId") UUID companyId
    );

    @Query("SELECT l FROM CompanyAccountLinkRecord l " +
           "WHERE l.tenantId = :tenantId AND l.sellerAccountId = :sellerAccountId " +
           "ORDER BY l.effectiveFrom DESC, l.version DESC")
    List<CompanyAccountLinkRecord> findHistoryBySellerAccount(
            @Param("tenantId") UUID tenantId,
            @Param("sellerAccountId") UUID sellerAccountId
    );
}
