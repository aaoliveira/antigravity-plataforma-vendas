package com.sellerintelligence.modules.company;

import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.modules.company.model.CompanyAccountLinkDto;
import com.sellerintelligence.modules.company.model.CompanyDto;
import com.sellerintelligence.modules.company.model.CreateCompanyCommand;
import com.sellerintelligence.modules.company.model.LinkAccountCommand;
import com.sellerintelligence.modules.company.model.UpdateCompanyCommand;
import com.sellerintelligence.modules.company.service.CompanyAccountLinkService;
import com.sellerintelligence.modules.company.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST para gestão de Empresas / Unidades de Negócio e Vínculo de Contas de Marketplaces.
 * Todos os endpoints são estritamente isolados por tenant_id.
 */
@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanyService companyService;
    private final CompanyAccountLinkService linkService;

    public CompanyController(CompanyService companyService, CompanyAccountLinkService linkService) {
        this.companyService = companyService;
        this.linkService = linkService;
    }

    @PostMapping
    public ResponseEntity<CompanyDto> createCompany(@Valid @RequestBody CreateCompanyCommand command) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        String userEmail = getAuthenticatedEmail();
        CompanyDto created = companyService.createCompany(tenantId, command, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<Page<CompanyDto>> listCompanies(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        Page<CompanyDto> companies = companyService.listCompanies(tenantId, search, activeOnly, pageable);
        return ResponseEntity.ok(companies);
    }

    @GetMapping("/all")
    public ResponseEntity<List<CompanyDto>> listAllCompanies() {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        List<CompanyDto> companies = companyService.listAllCompanies(tenantId);
        return ResponseEntity.ok(companies);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyDto> getCompany(@PathVariable UUID id) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return companyService.getCompany(tenantId, id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyDto> updateCompany(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCompanyCommand command
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        String userEmail = getAuthenticatedEmail();
        CompanyDto updated = companyService.updateCompany(tenantId, id, command, userEmail);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateCompany(@PathVariable UUID id) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        String userEmail = getAuthenticatedEmail();
        companyService.deactivateCompany(tenantId, id, userEmail);
        return ResponseEntity.noContent().build();
    }

    // --- VÍNCULOS DE CONTAS ---

    @PostMapping("/{companyId}/links")
    public ResponseEntity<CompanyAccountLinkDto> linkAccount(
            @PathVariable UUID companyId,
            @Valid @RequestBody LinkAccountCommand command
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        String userEmail = getAuthenticatedEmail();
        CompanyAccountLinkDto link = linkService.linkAccount(tenantId, companyId, command, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(link);
    }

    @DeleteMapping("/links/{linkId}")
    public ResponseEntity<Void> unlinkAccount(@PathVariable UUID linkId) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        String userEmail = getAuthenticatedEmail();
        linkService.unlinkAccount(tenantId, linkId, userEmail);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{companyId}/accounts")
    public ResponseEntity<List<CompanyAccountLinkDto>> listAccountsByCompany(@PathVariable UUID companyId) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        List<CompanyAccountLinkDto> accounts = linkService.listActiveAccountsByCompany(tenantId, companyId);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/accounts/{sellerAccountId}/history")
    public ResponseEntity<List<CompanyAccountLinkDto>> getAccountLinkHistory(@PathVariable UUID sellerAccountId) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        List<CompanyAccountLinkDto> history = linkService.getAccountLinkHistory(tenantId, sellerAccountId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/accounts/{sellerAccountId}/active")
    public ResponseEntity<CompanyAccountLinkDto> getActiveLink(@PathVariable UUID sellerAccountId) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return linkService.getActiveLink(tenantId, sellerAccountId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private String getAuthenticatedEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "system@sellerintelligence.com";
    }
}
