package br.com.sellerintelligence.tenancy.domain;

import br.com.sellerintelligence.audit.domain.AuditResult;
import br.com.sellerintelligence.audit.domain.AuditService;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserRepository;
import br.com.sellerintelligence.identity.domain.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public OrganizationService(OrganizationRepository organizationRepository,
                               MembershipRepository membershipRepository,
                               UserRepository userRepository,
                               AuditService auditService) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Organization createOrganization(String name, String slug, User creator) {
        Objects.requireNonNull(name, "Organization name must not be null");
        Objects.requireNonNull(slug, "Organization slug must not be null");
        Objects.requireNonNull(creator, "Creator user must not be null");

        String normalizedSlug = slug.trim().toLowerCase();
        if (organizationRepository.existsBySlug(normalizedSlug)) {
            throw new IllegalArgumentException("Organization with slug '" + normalizedSlug + "' already exists");
        }

        Organization org = new Organization(name.trim(), normalizedSlug);
        org = organizationRepository.save(org);

        Membership membership = new Membership(org, creator, Role.OWNER, MembershipStatus.ACTIVE);
        membershipRepository.save(membership);

        auditService.recordEvent(
                org.getId(),
                creator.getId().toString(),
                "ORGANIZATION_CREATED",
                "Organization",
                org.getId().toString(),
                "Created organization '" + org.getName() + "' with slug '" + org.getSlug() + "'",
                AuditResult.SUCCESS
        );

        return org;
    }

    @Transactional(readOnly = true)
    public List<Membership> getUserMemberships(UUID userId) {
        return membershipRepository.findActiveMembershipsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Organization getOrganizationForUser(UUID organizationId, UUID userId) {
        Membership membership = membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .orElseThrow(() -> {
                    auditService.recordEvent(
                            organizationId,
                            userId.toString(),
                            "ACCESS_DENIED",
                            "Organization",
                            organizationId.toString(),
                            "Unauthorized attempt to access organization details",
                            AuditResult.DENIED
                    );
                    return new TenantAccessDeniedException("Organization not accessible or not found");
                });

        return membership.getOrganization();
    }

    @Transactional(readOnly = true)
    public Optional<Membership> findActiveMembership(UUID organizationId, String keycloakSub) {
        return membershipRepository.findByOrganizationIdAndUserKeycloakSub(organizationId, keycloakSub)
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Membership> getOrganizationMembers(UUID organizationId, UUID requesterUserId) {
        verifyMembership(organizationId, requesterUserId);
        return membershipRepository.findByOrganizationIdWithUser(organizationId);
    }

    @Transactional
    public Membership addOrInviteMember(UUID organizationId, String email, String name, Role role, User actor) {
        Membership actorMembership = verifyMembership(organizationId, actor.getId());

        if (!actorMembership.getRole().canManageMembers()) {
            auditService.recordEvent(
                    organizationId,
                    actor.getId().toString(),
                    "ACCESS_DENIED",
                    "Membership",
                    null,
                    "User with role " + actorMembership.getRole() + " attempted to invite member",
                    AuditResult.DENIED
            );
            throw new TenantAccessDeniedException("Insufficient permissions to invite members");
        }

        if (!actorMembership.getRole().canAssignRole(role)) {
            auditService.recordEvent(
                    organizationId,
                    actor.getId().toString(),
                    "ACCESS_DENIED",
                    "Membership",
                    null,
                    "User with role " + actorMembership.getRole() + " attempted to assign disallowed role " + role,
                    AuditResult.DENIED
            );
            throw new TenantAccessDeniedException("Cannot assign role higher than or equal to current role");
        }

        String normalizedEmail = email.trim().toLowerCase();
        User targetUser = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> {
                    User newUser = new User(
                            "invited-" + UUID.randomUUID(),
                            normalizedEmail,
                            name != null && !name.isBlank() ? name.trim() : normalizedEmail
                    );
                    newUser.setStatus(UserStatus.INACTIVE);
                    return userRepository.save(newUser);
                });

        if (membershipRepository.existsByOrganizationIdAndUserIdAndStatus(organizationId, targetUser.getId(), MembershipStatus.ACTIVE)) {
            throw new IllegalArgumentException("User is already an active member of this organization");
        }

        Organization org = actorMembership.getOrganization();
        Membership newMembership = new Membership(org, targetUser, role, MembershipStatus.ACTIVE);
        newMembership = membershipRepository.save(newMembership);

        auditService.recordEvent(
                organizationId,
                actor.getId().toString(),
                "MEMBER_INVITED",
                "Membership",
                newMembership.getId().toString(),
                "Added user " + targetUser.getEmail() + " with role " + role,
                AuditResult.SUCCESS
        );

        return newMembership;
    }

    @Transactional
    public Membership updateMemberRole(UUID organizationId, UUID targetUserId, Role newRole, User actor) {
        Membership actorMembership = verifyMembership(organizationId, actor.getId());

        if (!actorMembership.getRole().canManageMembers()) {
            auditService.recordEvent(
                    organizationId,
                    actor.getId().toString(),
                    "ACCESS_DENIED",
                    "Membership",
                    targetUserId.toString(),
                    "Role update denied: insufficient permissions",
                    AuditResult.DENIED
            );
            throw new TenantAccessDeniedException("Insufficient permissions to update member roles");
        }

        if (!actorMembership.getRole().canAssignRole(newRole)) {
            auditService.recordEvent(
                    organizationId,
                    actor.getId().toString(),
                    "ACCESS_DENIED",
                    "Membership",
                    targetUserId.toString(),
                    "Role update denied: cannot assign role " + newRole,
                    AuditResult.DENIED
            );
            throw new TenantAccessDeniedException("Cannot assign role higher than or equal to current role");
        }

        Membership targetMembership = membershipRepository.findByOrganizationIdAndUserId(organizationId, targetUserId)
                .orElseThrow(() -> new TenantAccessDeniedException("Member not found in organization"));

        if (targetMembership.getRole() == Role.OWNER && newRole != Role.OWNER) {
            long ownerCount = membershipRepository.countByOrganizationIdAndRole(organizationId, Role.OWNER);
            if (ownerCount <= 1) {
                throw new IllegalArgumentException("Cannot demote the only OWNER of the organization");
            }
        }

        Role oldRole = targetMembership.getRole();
        targetMembership.setRole(newRole);
        targetMembership = membershipRepository.save(targetMembership);

        auditService.recordEvent(
                organizationId,
                actor.getId().toString(),
                "MEMBER_ROLE_CHANGED",
                "Membership",
                targetMembership.getId().toString(),
                "Changed role of user " + targetUserId + " from " + oldRole + " to " + newRole,
                AuditResult.SUCCESS
        );

        return targetMembership;
    }

    @Transactional
    public void removeMember(UUID organizationId, UUID targetUserId, User actor) {
        Membership actorMembership = verifyMembership(organizationId, actor.getId());

        if (!actorMembership.getRole().canManageMembers() && !actor.getId().equals(targetUserId)) {
            auditService.recordEvent(
                    organizationId,
                    actor.getId().toString(),
                    "ACCESS_DENIED",
                    "Membership",
                    targetUserId.toString(),
                    "Member removal denied: insufficient permissions",
                    AuditResult.DENIED
            );
            throw new TenantAccessDeniedException("Insufficient permissions to remove member");
        }

        Membership targetMembership = membershipRepository.findByOrganizationIdAndUserId(organizationId, targetUserId)
                .orElseThrow(() -> new TenantAccessDeniedException("Member not found in organization"));

        if (targetMembership.getRole() == Role.OWNER) {
            long ownerCount = membershipRepository.countByOrganizationIdAndRole(organizationId, Role.OWNER);
            if (ownerCount <= 1) {
                throw new IllegalArgumentException("Cannot remove the only OWNER of the organization");
            }
        }

        membershipRepository.delete(targetMembership);

        auditService.recordEvent(
                organizationId,
                actor.getId().toString(),
                "MEMBER_REMOVED",
                "Membership",
                targetMembership.getId().toString(),
                "Removed user " + targetUserId + " from organization",
                AuditResult.SUCCESS
        );
    }

    private Membership verifyMembership(UUID organizationId, UUID userId) {
        return membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .orElseThrow(() -> {
                    auditService.recordEvent(
                            organizationId,
                            userId.toString(),
                            "ACCESS_DENIED",
                            "Organization",
                            organizationId.toString(),
                            "Attempted operation on inaccessible organization",
                            AuditResult.DENIED
                    );
                    return new TenantAccessDeniedException("Organization not accessible or not found");
                });
    }
}
