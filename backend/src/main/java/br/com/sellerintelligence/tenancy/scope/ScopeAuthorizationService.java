package br.com.sellerintelligence.tenancy.scope;

import br.com.sellerintelligence.audit.domain.AuditResult;
import br.com.sellerintelligence.audit.domain.AuditService;
import br.com.sellerintelligence.tenancy.company.Company;
import br.com.sellerintelligence.tenancy.domain.Membership;
import br.com.sellerintelligence.tenancy.domain.MembershipRepository;
import br.com.sellerintelligence.tenancy.domain.Role;
import br.com.sellerintelligence.tenancy.domain.TenantAccessDeniedException;
import br.com.sellerintelligence.tenancy.store.Store;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ScopeAuthorizationService {

    private final MembershipRepository membershipRepository;
    private final MembershipScopeRepository membershipScopeRepository;
    private final AuditService auditService;

    public ScopeAuthorizationService(MembershipRepository membershipRepository,
                                     MembershipScopeRepository membershipScopeRepository,
                                     AuditService auditService) {
        this.membershipRepository = membershipRepository;
        this.membershipScopeRepository = membershipScopeRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public boolean canAccessCompany(UUID tenantId, UUID userId, UUID companyId) {
        Membership membership = membershipRepository.findByOrganizationIdAndUserId(tenantId, userId)
                .orElseThrow(() -> new TenantAccessDeniedException("Membership not found in tenant"));

        if (membership.getRole() == Role.OWNER || membership.getRole() == Role.ADMIN) {
            return true;
        }

        List<MembershipScope> scopes = membershipScopeRepository.findByMembershipId(membership.getId());
        if (scopes.isEmpty()) {
            return true; // No granular restrictions configured = full tenant access
        }

        return scopes.stream()
                .anyMatch(s -> s.getScopeType() == ScopeType.COMPANY && s.getScopeId().equals(companyId));
    }

    @Transactional(readOnly = true)
    public void checkCompanyAccess(UUID tenantId, UUID userId, UUID companyId) {
        if (!canAccessCompany(tenantId, userId, companyId)) {
            auditService.recordEvent(
                    tenantId,
                    userId.toString(),
                    "ACCESS_DENIED",
                    "Company",
                    companyId.toString(),
                    "User access restricted by company scope",
                    AuditResult.DENIED
            );
            throw new TenantAccessDeniedException("Access to this company is restricted by your user scope");
        }
    }

    @Transactional(readOnly = true)
    public boolean canAccessStore(UUID tenantId, UUID userId, Store store) {
        Membership membership = membershipRepository.findByOrganizationIdAndUserId(tenantId, userId)
                .orElseThrow(() -> new TenantAccessDeniedException("Membership not found in tenant"));

        if (membership.getRole() == Role.OWNER || membership.getRole() == Role.ADMIN) {
            return true;
        }

        List<MembershipScope> scopes = membershipScopeRepository.findByMembershipId(membership.getId());
        if (scopes.isEmpty()) {
            return true; // No granular restrictions configured = full tenant access
        }

        boolean storeMatch = scopes.stream()
                .anyMatch(s -> s.getScopeType() == ScopeType.STORE && s.getScopeId().equals(store.getId()));
        if (storeMatch) {
            return true;
        }

        // Access to company also grants access to stores of that company
        UUID companyId = store.getCompany().getId();
        return scopes.stream()
                .anyMatch(s -> s.getScopeType() == ScopeType.COMPANY && s.getScopeId().equals(companyId));
    }

    @Transactional(readOnly = true)
    public void checkStoreAccess(UUID tenantId, UUID userId, Store store) {
        if (!canAccessStore(tenantId, userId, store)) {
            auditService.recordEvent(
                    tenantId,
                    userId.toString(),
                    "ACCESS_DENIED",
                    "Store",
                    store.getId().toString(),
                    "User access restricted by store scope",
                    AuditResult.DENIED
            );
            throw new TenantAccessDeniedException("Access to this store is restricted by your user scope");
        }
    }

    @Transactional(readOnly = true)
    public Set<UUID> getAllowedCompanyIds(UUID tenantId, UUID userId) {
        Membership membership = membershipRepository.findByOrganizationIdAndUserId(tenantId, userId)
                .orElseThrow(() -> new TenantAccessDeniedException("Membership not found in tenant"));

        if (membership.getRole() == Role.OWNER || membership.getRole() == Role.ADMIN) {
            return Set.of(); // Empty set indicates unrestricted access
        }

        List<MembershipScope> scopes = membershipScopeRepository.findByMembershipIdAndScopeType(
                membership.getId(), ScopeType.COMPANY);
        return scopes.stream().map(MembershipScope::getScopeId).collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Set<UUID> getAllowedStoreIds(UUID tenantId, UUID userId) {
        Membership membership = membershipRepository.findByOrganizationIdAndUserId(tenantId, userId)
                .orElseThrow(() -> new TenantAccessDeniedException("Membership not found in tenant"));

        if (membership.getRole() == Role.OWNER || membership.getRole() == Role.ADMIN) {
            return Set.of(); // Empty set indicates unrestricted access
        }

        List<MembershipScope> scopes = membershipScopeRepository.findByMembershipIdAndScopeType(
                membership.getId(), ScopeType.STORE);
        return scopes.stream().map(MembershipScope::getScopeId).collect(Collectors.toSet());
    }
}
