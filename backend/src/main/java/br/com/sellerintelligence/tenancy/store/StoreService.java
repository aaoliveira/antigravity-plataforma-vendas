package br.com.sellerintelligence.tenancy.store;

import br.com.sellerintelligence.audit.domain.AuditResult;
import br.com.sellerintelligence.audit.domain.AuditService;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.tenancy.company.Company;
import br.com.sellerintelligence.tenancy.company.CompanyRepository;
import br.com.sellerintelligence.tenancy.domain.Membership;
import br.com.sellerintelligence.tenancy.domain.MembershipRepository;
import br.com.sellerintelligence.tenancy.domain.TenantAccessDeniedException;
import br.com.sellerintelligence.tenancy.scope.ScopeAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class StoreService {

    private final StoreRepository storeRepository;
    private final CompanyRepository companyRepository;
    private final MembershipRepository membershipRepository;
    private final ScopeAuthorizationService scopeAuthorizationService;
    private final AuditService auditService;

    public StoreService(StoreRepository storeRepository,
                        CompanyRepository companyRepository,
                        MembershipRepository membershipRepository,
                        ScopeAuthorizationService scopeAuthorizationService,
                        AuditService auditService) {
        this.storeRepository = storeRepository;
        this.companyRepository = companyRepository;
        this.membershipRepository = membershipRepository;
        this.scopeAuthorizationService = scopeAuthorizationService;
        this.auditService = auditService;
    }

    @Transactional
    public Store createStore(UUID tenantId, UUID companyId, Marketplace marketplace,
                             String internalName, String metadata, User actor) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(companyId, "companyId must not be null");
        Objects.requireNonNull(marketplace, "marketplace must not be null");
        Objects.requireNonNull(internalName, "internalName must not be null");

        Membership membership = verifyActiveMembership(tenantId, actor.getId());
        if (!membership.getRole().canManageMembers()) {
            auditService.recordEvent(
                    tenantId,
                    actor.getId().toString(),
                    "ACCESS_DENIED",
                    "Store",
                    null,
                    "User attempted to create store without management role",
                    AuditResult.DENIED
            );
            throw new TenantAccessDeniedException("Insufficient permissions to create stores");
        }

        Company company = companyRepository.findByIdAndTenantId(companyId, tenantId)
                .orElseThrow(() -> {
                    auditService.recordEvent(
                            tenantId,
                            actor.getId().toString(),
                            "ACCESS_DENIED",
                            "Store",
                            null,
                            "Attempted to link store to company from another tenant or non-existent company",
                            AuditResult.DENIED
                    );
                    return new TenantAccessDeniedException("Target company does not belong to the current organization");
                });

        Store store = new Store(tenantId, company, marketplace, internalName.trim());
        store.setMetadata(metadata);
        store = storeRepository.save(store);

        auditService.recordEvent(
                tenantId,
                actor.getId().toString(),
                "STORE_CREATED",
                "Store",
                store.getId().toString(),
                "Created store " + store.getInternalName() + " for marketplace " + marketplace,
                AuditResult.SUCCESS
        );

        return store;
    }

    @Transactional(readOnly = true)
    public List<Store> getStoresForUser(UUID tenantId, UUID userId, UUID optionalCompanyId) {
        verifyActiveMembership(tenantId, userId);

        List<Store> candidateStores;
        if (optionalCompanyId != null) {
            candidateStores = storeRepository.findByTenantIdAndCompanyIdWithCompany(tenantId, optionalCompanyId);
        } else {
            candidateStores = storeRepository.findByTenantIdWithCompany(tenantId);
        }

        return candidateStores.stream()
                .filter(s -> scopeAuthorizationService.canAccessStore(tenantId, userId, s))
                .toList();
    }

    @Transactional(readOnly = true)
    public Store getStoreById(UUID tenantId, UUID storeId, UUID userId) {
        verifyActiveMembership(tenantId, userId);

        Store store = storeRepository.findByIdAndTenantIdWithCompany(storeId, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Store not found or not accessible"));

        scopeAuthorizationService.checkStoreAccess(tenantId, userId, store);
        return store;
    }

    @Transactional
    public Store updateStore(UUID tenantId, UUID storeId, String internalName,
                             String externalAccountId, ConnectionStatus connectionStatus,
                             String metadata, User actor) {
        Membership membership = verifyActiveMembership(tenantId, actor.getId());
        if (!membership.getRole().canManageMembers()) {
            throw new TenantAccessDeniedException("Insufficient permissions to update stores");
        }

        Store store = storeRepository.findByIdAndTenantIdWithCompany(storeId, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Store not found or not accessible"));

        if (internalName != null && !internalName.isBlank()) {
            store.setInternalName(internalName.trim());
        }
        if (externalAccountId != null) {
            store.setExternalAccountId(externalAccountId.trim());
        }
        if (connectionStatus != null) {
            store.setConnectionStatus(connectionStatus);
        }
        if (metadata != null) {
            store.setMetadata(metadata);
        }

        store = storeRepository.save(store);

        auditService.recordEvent(
                tenantId,
                actor.getId().toString(),
                "STORE_UPDATED",
                "Store",
                store.getId().toString(),
                "Updated store " + store.getInternalName(),
                AuditResult.SUCCESS
        );

        return store;
    }

    @Transactional
    public Store setStoreStatus(UUID tenantId, UUID storeId, StoreStatus newStatus, User actor) {
        Membership membership = verifyActiveMembership(tenantId, actor.getId());
        if (!membership.getRole().canManageMembers()) {
            throw new TenantAccessDeniedException("Insufficient permissions to modify store status");
        }

        Store store = storeRepository.findByIdAndTenantIdWithCompany(storeId, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Store not found or not accessible"));

        store.setStatus(newStatus);
        store = storeRepository.save(store);

        auditService.recordEvent(
                tenantId,
                actor.getId().toString(),
                "STORE_STATUS_CHANGED",
                "Store",
                store.getId().toString(),
                "Changed store status to " + newStatus,
                AuditResult.SUCCESS
        );

        return store;
    }

    private Membership verifyActiveMembership(UUID tenantId, UUID userId) {
        return membershipRepository.findByOrganizationIdAndUserId(tenantId, userId)
                .filter(m -> m.getStatus().name().equals("ACTIVE"))
                .orElseThrow(() -> new TenantAccessDeniedException("Access denied: user is not an active member of this organization"));
    }
}
