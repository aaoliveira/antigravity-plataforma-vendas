package br.com.sellerintelligence.tenancy.company;

import br.com.sellerintelligence.audit.domain.AuditResult;
import br.com.sellerintelligence.audit.domain.AuditService;
import br.com.sellerintelligence.identity.domain.User;
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
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final MembershipRepository membershipRepository;
    private final ScopeAuthorizationService scopeAuthorizationService;
    private final AuditService auditService;

    public CompanyService(CompanyRepository companyRepository,
                          MembershipRepository membershipRepository,
                          ScopeAuthorizationService scopeAuthorizationService,
                          AuditService auditService) {
        this.companyRepository = companyRepository;
        this.membershipRepository = membershipRepository;
        this.scopeAuthorizationService = scopeAuthorizationService;
        this.auditService = auditService;
    }

    @Transactional
    public Company createCompany(UUID tenantId, String cnpj, String tradeName,
                                 String legalName, TaxRegime taxRegime, User actor) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(cnpj, "cnpj must not be null");
        Objects.requireNonNull(tradeName, "tradeName must not be null");

        Membership membership = verifyActiveMembership(tenantId, actor.getId());
        if (!membership.getRole().canManageMembers()) {
            auditService.recordEvent(
                    tenantId,
                    actor.getId().toString(),
                    "ACCESS_DENIED",
                    "Company",
                    null,
                    "User attempted to create company without management role",
                    AuditResult.DENIED
            );
            throw new TenantAccessDeniedException("Insufficient permissions to create companies");
        }

        if (!CnpjValidator.isValid(cnpj)) {
            throw new IllegalArgumentException("Invalid CNPJ: must contain 14 digits and valid check digits");
        }

        String formattedCnpj = CnpjValidator.format(cnpj);
        if (companyRepository.existsByTenantIdAndCnpj(tenantId, formattedCnpj)) {
            throw new IllegalArgumentException("Company with CNPJ " + formattedCnpj + " already exists in this organization");
        }

        Company company = new Company(tenantId, formattedCnpj, tradeName.trim(), legalName != null ? legalName.trim() : null, taxRegime);
        company = companyRepository.save(company);

        auditService.recordEvent(
                tenantId,
                actor.getId().toString(),
                "COMPANY_CREATED",
                "Company",
                company.getId().toString(),
                "Created company " + company.getTradeName() + " (CNPJ: " + company.getCnpj() + ")",
                AuditResult.SUCCESS
        );

        return company;
    }

    @Transactional(readOnly = true)
    public List<Company> getCompaniesForUser(UUID tenantId, UUID userId) {
        verifyActiveMembership(tenantId, userId);
        List<Company> allCompanies = companyRepository.findByTenantId(tenantId);

        Set<UUID> allowedCompanyIds = scopeAuthorizationService.getAllowedCompanyIds(tenantId, userId);
        if (allowedCompanyIds.isEmpty()) {
            return allCompanies;
        }

        return allCompanies.stream()
                .filter(c -> allowedCompanyIds.contains(c.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Company getCompanyById(UUID tenantId, UUID companyId, UUID userId) {
        verifyActiveMembership(tenantId, userId);
        scopeAuthorizationService.checkCompanyAccess(tenantId, userId, companyId);

        return companyRepository.findByIdAndTenantId(companyId, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Company not found or not accessible"));
    }

    @Transactional
    public Company updateCompany(UUID tenantId, UUID companyId, String tradeName,
                                 String legalName, TaxRegime taxRegime, User actor) {
        Membership membership = verifyActiveMembership(tenantId, actor.getId());
        if (!membership.getRole().canManageMembers()) {
            throw new TenantAccessDeniedException("Insufficient permissions to update companies");
        }

        Company company = companyRepository.findByIdAndTenantId(companyId, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Company not found or not accessible"));

        if (tradeName != null && !tradeName.isBlank()) {
            company.setTradeName(tradeName.trim());
        }
        if (legalName != null) {
            company.setLegalName(legalName.trim());
        }
        if (taxRegime != null) {
            company.setTaxRegime(taxRegime);
        }

        company = companyRepository.save(company);

        auditService.recordEvent(
                tenantId,
                actor.getId().toString(),
                "COMPANY_UPDATED",
                "Company",
                company.getId().toString(),
                "Updated company " + company.getTradeName(),
                AuditResult.SUCCESS
        );

        return company;
    }

    @Transactional
    public Company setCompanyStatus(UUID tenantId, UUID companyId, CompanyStatus newStatus, User actor) {
        Membership membership = verifyActiveMembership(tenantId, actor.getId());
        if (!membership.getRole().canManageMembers()) {
            throw new TenantAccessDeniedException("Insufficient permissions to modify company status");
        }

        Company company = companyRepository.findByIdAndTenantId(companyId, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Company not found or not accessible"));

        company.setStatus(newStatus);
        company = companyRepository.save(company);

        auditService.recordEvent(
                tenantId,
                actor.getId().toString(),
                "COMPANY_STATUS_CHANGED",
                "Company",
                company.getId().toString(),
                "Changed company status to " + newStatus,
                AuditResult.SUCCESS
        );

        return company;
    }

    private Membership verifyActiveMembership(UUID tenantId, UUID userId) {
        return membershipRepository.findByOrganizationIdAndUserId(tenantId, userId)
                .filter(m -> m.getStatus().name().equals("ACTIVE"))
                .orElseThrow(() -> new TenantAccessDeniedException("Access denied: user is not an active member of this organization"));
    }
}
