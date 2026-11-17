package br.com.sellerintelligence.tenancy.invitation;

import br.com.sellerintelligence.audit.domain.AuditResult;
import br.com.sellerintelligence.audit.domain.AuditService;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserRepository;
import br.com.sellerintelligence.tenancy.company.CompanyRepository;
import br.com.sellerintelligence.tenancy.domain.*;
import br.com.sellerintelligence.tenancy.scope.MembershipScope;
import br.com.sellerintelligence.tenancy.scope.MembershipScopeRepository;
import br.com.sellerintelligence.tenancy.scope.ScopeType;
import br.com.sellerintelligence.tenancy.store.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class InvitationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserInvitationRepository invitationRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipScopeRepository membershipScopeRepository;
    private final OrganizationRepository organizationRepository;
    private final CompanyRepository companyRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public InvitationService(UserInvitationRepository invitationRepository,
                             MembershipRepository membershipRepository,
                             MembershipScopeRepository membershipScopeRepository,
                             OrganizationRepository organizationRepository,
                             CompanyRepository companyRepository,
                             StoreRepository storeRepository,
                             UserRepository userRepository,
                             AuditService auditService) {
        this.invitationRepository = invitationRepository;
        this.membershipRepository = membershipRepository;
        this.membershipScopeRepository = membershipScopeRepository;
        this.organizationRepository = organizationRepository;
        this.companyRepository = companyRepository;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public UserInvitation createInvitation(UUID tenantId, String email, Role role,
                                           List<UUID> allowedCompanyIds, List<UUID> allowedStoreIds,
                                           User actor) {
        Membership actorMembership = verifyActiveMembership(tenantId, actor.getId());
        if (!actorMembership.getRole().canManageMembers()) {
            throw new TenantAccessDeniedException("Insufficient permissions to invite users");
        }

        if (!actorMembership.getRole().canAssignRole(role)) {
            throw new TenantAccessDeniedException("Cannot invite a user with role higher than or equal to current role");
        }

        // Validate allowed company IDs belong to tenant
        if (allowedCompanyIds != null && !allowedCompanyIds.isEmpty()) {
            for (UUID compId : allowedCompanyIds) {
                if (companyRepository.findByIdAndTenantId(compId, tenantId).isEmpty()) {
                    throw new IllegalArgumentException("Company ID " + compId + " does not belong to this organization");
                }
            }
        }

        // Validate allowed store IDs belong to tenant
        if (allowedStoreIds != null && !allowedStoreIds.isEmpty()) {
            for (UUID stId : allowedStoreIds) {
                if (storeRepository.findByIdAndTenantIdWithCompany(stId, tenantId).isEmpty()) {
                    throw new IllegalArgumentException("Store ID " + stId + " does not belong to this organization");
                }
            }
        }

        String token = generateSecureToken();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(7);

        String companyIdsStr = allowedCompanyIds != null && !allowedCompanyIds.isEmpty()
                ? String.join(",", allowedCompanyIds.stream().map(UUID::toString).toList())
                : null;

        String storeIdsStr = allowedStoreIds != null && !allowedStoreIds.isEmpty()
                ? String.join(",", allowedStoreIds.stream().map(UUID::toString).toList())
                : null;

        UserInvitation invitation = new UserInvitation(
                tenantId,
                email.trim().toLowerCase(),
                role,
                token,
                companyIdsStr,
                storeIdsStr,
                expiresAt,
                actor.getId()
        );

        invitation = invitationRepository.save(invitation);

        auditService.recordEvent(
                tenantId,
                actor.getId().toString(),
                "INVITATION_CREATED",
                "UserInvitation",
                invitation.getId().toString(),
                "Invited " + invitation.getEmail() + " as " + role,
                AuditResult.SUCCESS
        );

        return invitation;
    }

    @Transactional
    public Membership acceptInvitation(String token, User acceptingUser) {
        UserInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invitation token not found"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("Invitation is no longer pending (status: " + invitation.getStatus() + ")");
        }

        if (invitation.isExpired()) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new IllegalArgumentException("Invitation token has expired");
        }

        Organization org = organizationRepository.findById(invitation.getTenantId())
                .orElseThrow(() -> new TenantAccessDeniedException("Organization no longer exists"));

        // Create or update Membership
        Membership membership = membershipRepository.findByOrganizationIdAndUserId(org.getId(), acceptingUser.getId())
                .orElseGet(() -> new Membership(org, acceptingUser, invitation.getRole(), MembershipStatus.ACTIVE));

        membership.setRole(invitation.getRole());
        membership.setStatus(MembershipStatus.ACTIVE);
        membership = membershipRepository.save(membership);

        // Populate Scopes if specified
        if (invitation.getAllowedCompanyIds() != null && !invitation.getAllowedCompanyIds().isBlank()) {
            for (String compIdStr : invitation.getAllowedCompanyIds().split(",")) {
                UUID compId = UUID.fromString(compIdStr.trim());
                if (!membershipScopeRepository.existsByMembershipIdAndScopeTypeAndScopeId(
                        membership.getId(), ScopeType.COMPANY, compId)) {
                    membershipScopeRepository.save(new MembershipScope(membership, ScopeType.COMPANY, compId));
                }
            }
        }

        if (invitation.getAllowedStoreIds() != null && !invitation.getAllowedStoreIds().isBlank()) {
            for (String storeIdStr : invitation.getAllowedStoreIds().split(",")) {
                UUID storeId = UUID.fromString(storeIdStr.trim());
                if (!membershipScopeRepository.existsByMembershipIdAndScopeTypeAndScopeId(
                        membership.getId(), ScopeType.STORE, storeId)) {
                    membershipScopeRepository.save(new MembershipScope(membership, ScopeType.STORE, storeId));
                }
            }
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(OffsetDateTime.now());
        invitationRepository.save(invitation);

        auditService.recordEvent(
                invitation.getTenantId(),
                acceptingUser.getId().toString(),
                "INVITATION_ACCEPTED",
                "Membership",
                membership.getId().toString(),
                "User " + acceptingUser.getEmail() + " accepted invitation as " + invitation.getRole(),
                AuditResult.SUCCESS
        );

        return membership;
    }

    @Transactional(readOnly = true)
    public List<UserInvitation> getPendingInvitations(UUID tenantId, UUID userId) {
        verifyActiveMembership(tenantId, userId);
        return invitationRepository.findByTenantIdAndStatus(tenantId, InvitationStatus.PENDING);
    }

    @Transactional
    public void revokeInvitation(UUID tenantId, UUID invitationId, User actor) {
        Membership actorMembership = verifyActiveMembership(tenantId, actor.getId());
        if (!actorMembership.getRole().canManageMembers()) {
            throw new TenantAccessDeniedException("Insufficient permissions to revoke invitations");
        }

        UserInvitation invitation = invitationRepository.findByIdAndTenantId(invitationId, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Invitation not found"));

        invitation.setStatus(InvitationStatus.REVOKED);
        invitationRepository.save(invitation);

        auditService.recordEvent(
                tenantId,
                actor.getId().toString(),
                "INVITATION_REVOKED",
                "UserInvitation",
                invitation.getId().toString(),
                "Revoked invitation for " + invitation.getEmail(),
                AuditResult.SUCCESS
        );
    }

    private Membership verifyActiveMembership(UUID tenantId, UUID userId) {
        return membershipRepository.findByOrganizationIdAndUserId(tenantId, userId)
                .filter(m -> m.getStatus().name().equals("ACTIVE"))
                .orElseThrow(() -> new TenantAccessDeniedException("Access denied: user is not an active member of this organization"));
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
