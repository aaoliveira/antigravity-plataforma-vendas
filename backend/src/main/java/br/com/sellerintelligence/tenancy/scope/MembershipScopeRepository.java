package br.com.sellerintelligence.tenancy.scope;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MembershipScopeRepository extends JpaRepository<MembershipScope, UUID> {

    List<MembershipScope> findByMembershipId(UUID membershipId);

    List<MembershipScope> findByMembershipIdAndScopeType(UUID membershipId, ScopeType scopeType);

    boolean existsByMembershipIdAndScopeTypeAndScopeId(UUID membershipId, ScopeType scopeType, UUID scopeId);

    void deleteByMembershipId(UUID membershipId);
}
