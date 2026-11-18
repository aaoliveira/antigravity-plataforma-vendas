package com.sellerintelligence.modules.tenancy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    List<Membership> findByUserIdAndStatus(UUID userId, String status);

    Optional<Membership> findByUserIdAndTenantIdAndStatus(UUID userId, UUID tenantId, String status);

    boolean existsByUserIdAndTenantIdAndStatus(UUID userId, UUID tenantId, String status);
}
