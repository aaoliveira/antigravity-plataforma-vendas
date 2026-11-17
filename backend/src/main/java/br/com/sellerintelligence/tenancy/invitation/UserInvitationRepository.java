package br.com.sellerintelligence.tenancy.invitation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserInvitationRepository extends JpaRepository<UserInvitation, UUID> {

    Optional<UserInvitation> findByToken(String token);

    List<UserInvitation> findByTenantIdAndStatus(UUID tenantId, InvitationStatus status);

    List<UserInvitation> findByTenantId(UUID tenantId);

    Optional<UserInvitation> findByIdAndTenantId(UUID id, UUID tenantId);
}
