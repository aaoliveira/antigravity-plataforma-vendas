package br.com.sellerintelligence.tenancy.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    @Query("SELECT m FROM Membership m JOIN FETCH m.organization WHERE m.user.id = :userId AND m.status = 'ACTIVE'")
    List<Membership> findActiveMembershipsByUserId(@Param("userId") UUID userId);

    @Query("SELECT m FROM Membership m JOIN FETCH m.user WHERE m.organization.id = :organizationId")
    List<Membership> findByOrganizationIdWithUser(@Param("organizationId") UUID organizationId);

    @Query("SELECT m FROM Membership m JOIN FETCH m.organization JOIN FETCH m.user " +
           "WHERE m.organization.id = :organizationId AND m.user.id = :userId")
    Optional<Membership> findByOrganizationIdAndUserId(@Param("organizationId") UUID organizationId, @Param("userId") UUID userId);

    @Query("SELECT m FROM Membership m JOIN FETCH m.organization JOIN FETCH m.user " +
           "WHERE m.organization.id = :organizationId AND m.user.keycloakSub = :keycloakSub")
    Optional<Membership> findByOrganizationIdAndUserKeycloakSub(@Param("organizationId") UUID organizationId, @Param("keycloakSub") String keycloakSub);

    boolean existsByOrganizationIdAndUserIdAndStatus(UUID organizationId, UUID userId, MembershipStatus status);

    long countByOrganizationIdAndRole(UUID organizationId, Role role);
}
