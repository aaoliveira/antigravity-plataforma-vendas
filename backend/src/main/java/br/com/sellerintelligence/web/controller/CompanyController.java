package br.com.sellerintelligence.web.controller;

import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserService;
import br.com.sellerintelligence.tenancy.company.Company;
import br.com.sellerintelligence.tenancy.company.CompanyService;
import br.com.sellerintelligence.tenancy.company.CompanyStatus;
import br.com.sellerintelligence.tenancy.domain.TenantContext;
import br.com.sellerintelligence.web.dto.CompanyResponse;
import br.com.sellerintelligence.web.dto.CreateCompanyRequest;
import br.com.sellerintelligence.web.dto.UpdateCompanyRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanyService companyService;
    private final UserService userService;

    public CompanyController(CompanyService companyService, UserService userService) {
        this.companyService = companyService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<CompanyResponse> createCompany(
            @Valid @RequestBody CreateCompanyRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = TenantContext.getTenantId();
        User actor = userService.resolveOrCreateUserFromJwt(jwt);

        Company company = companyService.createCompany(
                tenantId,
                request.cnpj(),
                request.tradeName(),
                request.legalName(),
                request.taxRegime(),
                actor
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(CompanyResponse.from(company));
    }

    @GetMapping
    public ResponseEntity<List<CompanyResponse>> getCompanies(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = TenantContext.getTenantId();
        User actor = userService.resolveOrCreateUserFromJwt(jwt);

        List<Company> companies = companyService.getCompaniesForUser(tenantId, actor.getId());
        List<CompanyResponse> responses = companies.stream()
                .map(CompanyResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponse> getCompany(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = TenantContext.getTenantId();
        User actor = userService.resolveOrCreateUserFromJwt(jwt);

        Company company = companyService.getCompanyById(tenantId, id, actor.getId());
        return ResponseEntity.ok(CompanyResponse.from(company));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyResponse> updateCompany(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCompanyRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = TenantContext.getTenantId();
        User actor = userService.resolveOrCreateUserFromJwt(jwt);

        Company company = companyService.updateCompany(
                tenantId,
                id,
                request.tradeName(),
                request.legalName(),
                request.taxRegime(),
                actor
        );

        return ResponseEntity.ok(CompanyResponse.from(company));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CompanyResponse> setCompanyStatus(
            @PathVariable UUID id,
            @RequestParam CompanyStatus status,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = TenantContext.getTenantId();
        User actor = userService.resolveOrCreateUserFromJwt(jwt);

        Company company = companyService.setCompanyStatus(tenantId, id, status, actor);
        return ResponseEntity.ok(CompanyResponse.from(company));
    }
}
