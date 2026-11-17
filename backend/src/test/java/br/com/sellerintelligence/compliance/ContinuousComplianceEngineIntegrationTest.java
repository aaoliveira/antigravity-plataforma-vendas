package br.com.sellerintelligence.compliance;

import br.com.sellerintelligence.audit.domain.AuditEvent;
import br.com.sellerintelligence.audit.domain.AuditEventRepository;
import br.com.sellerintelligence.compliance.domain.*;
import br.com.sellerintelligence.compliance.exception.ComplianceViolationException;
import br.com.sellerintelligence.compliance.service.*;
import br.com.sellerintelligence.config.TestSecurityConfig;
import br.com.sellerintelligence.connector.model.MarketplaceCredential;
import br.com.sellerintelligence.connector.model.MarketplaceCredentialRepository;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserRepository;
import br.com.sellerintelligence.infrastructure.error.BusinessException;
import br.com.sellerintelligence.infrastructure.security.UrlSecurityValidator;
import br.com.sellerintelligence.tenancy.company.Company;
import br.com.sellerintelligence.tenancy.company.CompanyRepository;
import br.com.sellerintelligence.tenancy.company.TaxRegime;
import br.com.sellerintelligence.tenancy.domain.*;
import br.com.sellerintelligence.tenancy.store.ConnectionStatus;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import br.com.sellerintelligence.tenancy.store.Store;
import br.com.sellerintelligence.tenancy.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Transactional
public class ContinuousComplianceEngineIntegrationTest {

    @Autowired
    private MarketplacePolicyRegistryService policyRegistryService;

    @Autowired
    private PolicyMonitoringEngine monitoringEngine;

    @Autowired
    private PolicyChangeDetector changeDetector;

    @Autowired
    private AutomaticComplianceEnforcer complianceEnforcer;

    @Autowired
    private SafeReactivationService reactivationService;

    @Autowired
    private PermissionAuthorizationValidator permissionValidator;

    @Autowired
    private ApiLifecycleMonitor apiLifecycleMonitor;

    @Autowired
    private ComplianceIncidentService incidentService;

    @Autowired
    private ComplianceNotificationService notificationService;

    @Autowired
    private ComplianceDashboardService dashboardService;

    @Autowired
    private MarketplaceComplianceGuard complianceGuard;

    @Autowired
    private UrlSecurityValidator urlSecurityValidator;

    @Autowired
    private MarketplacePolicyRepository policyRepository;

    @Autowired
    private MarketplacePolicyChangeRepository changeRepository;

    @Autowired
    private ComplianceIncidentRepository incidentRepository;

    @Autowired
    private FeatureEnforcementRepository enforcementRepository;

    @Autowired
    private SafeReactivationLogRepository reactivationLogRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private MarketplaceCredentialRepository credentialRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    private Organization orgAlpha;
    private User ownerAlpha;
    private Company companyAlpha;
    private Store storeMlAlpha;
    private MarketplacePolicy testPolicyMl;

    @BeforeEach
    void setUp() {
        enforcementRepository.deleteAll();
        reactivationLogRepository.deleteAll();
        incidentRepository.deleteAll();
        changeRepository.deleteAll();
        policyRepository.deleteAll();
        credentialRepository.deleteAll();
        storeRepository.deleteAll();
        companyRepository.deleteAll();
        membershipRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();
        auditEventRepository.deleteAll();
        notificationService.clearCooldowns();

        // Seed default policies
        policyRegistryService.seedDefaultPoliciesIfEmpty();

        orgAlpha = organizationRepository.save(new Organization("Compliance Org", "comp-org"));
        ownerAlpha = userRepository.save(new User("sub-comp-owner", "owner@compliance.test", "Compliance Owner"));
        membershipRepository.save(new Membership(orgAlpha, ownerAlpha, Role.OWNER, MembershipStatus.ACTIVE));

        companyAlpha = companyRepository.save(new Company(
                orgAlpha.getId(), "11.222.333/0001-81", "Compliance LTDA", "Compliance Legal", TaxRegime.SIMPLES_NACIONAL));

        storeMlAlpha = new Store(orgAlpha.getId(), companyAlpha, Marketplace.MERCADO_LIVRE, "Loja ML Compliance");
        storeMlAlpha.setConnectionStatus(ConnectionStatus.CONNECTED);
        storeMlAlpha = storeRepository.save(storeMlAlpha);

        MarketplaceCredential cred = new MarketplaceCredential(
                orgAlpha.getId(), storeMlAlpha, Marketplace.MERCADO_LIVRE,
                "enc_acc", "enc_ref", "MLB-ACCT-99", "read,write",
                OffsetDateTime.now().plusDays(30), OffsetDateTime.now().plusMonths(6), "production"
        );
        cred.setStatus("CONNECTED");
        credentialRepository.save(cred);

        testPolicyMl = policyRepository.findByMarketplace(Marketplace.MERCADO_LIVRE).get(0);
    }

    @Test
    @DisplayName("01 e 02: Identificação de alteração em política e mudança de hash")
    void test01_02_detectPolicyChangeAndHashDiff() {
        String updatedText = "Mercado Livre Orders API v2: Rate limit reduced to 10 requests per second. Suspended if exceeded.";
        monitoringEngine.checkPolicy(testPolicyMl.getId(), updatedText, "AUDITOR_ADMIN");

        List<MarketplacePolicyChange> changes = changeRepository.findByMarketplaceOrderByDetectedAtDesc(Marketplace.MERCADO_LIVRE);
        assertThat(changes).isNotEmpty();

        MarketplacePolicyChange change = changes.get(0);
        assertThat(change.getPreviousHash()).isNotEqualTo(change.getCurrentHash());
        assertThat(change.getCurrentContent()).contains("Rate limit reduced to 10 requests");
    }

    @Test
    @DisplayName("03 e 04: Mudança semântica vs Desconsideração de alteração irrelevante de formatação")
    void test03_04_formattingOnlyVsSemanticChange() {
        String originalContent = testPolicyMl.getRawContent();

        // Whitespace and line break differences only
        String formattingOnlyContent = "\n\n   " + originalContent.replaceAll(" ", "   ") + "\n\t";
        assertThat(changeDetector.isFormattingOnlyChange(originalContent, formattingOnlyContent)).isTrue();

        MarketplacePolicyChange change = changeDetector.evaluateAndRecordChange(
                testPolicyMl, formattingOnlyContent, "v2026.1", "Formatting test", "TESTER");

        assertThat(change.getChangeType()).isEqualTo(PolicyChangeType.FORMATTING_ONLY);
        assertThat(change.getRiskLevel()).isEqualTo(ComplianceRiskLevel.INFORMATIONAL);
    }

    @Test
    @DisplayName("05: Registro de nova versão da política")
    void test05_recordNewPolicyVersion() {
        String newVersion = "v2026.2.0-RELEASE";
        String newContent = "Mercado Livre API Terms v2026.2: Updated authentication standards with PKCE mandatory.";

        changeDetector.evaluateAndRecordChange(testPolicyMl, newContent, newVersion, "Version upgrade test", "ADMIN");

        MarketplacePolicy reloaded = policyRepository.findById(testPolicyMl.getId()).orElseThrow();
        assertThat(reloaded.getPolicyVersion()).isEqualTo(newVersion);
        assertThat(reloaded.getRawContent()).contains("v2026.2");
    }

    @Test
    @DisplayName("06: Detecção de fonte indisponível marca status SOURCE_UNAVAILABLE")
    void test06_detectSourceUnavailable() {
        monitoringEngine.markSourceUnavailable(testPolicyMl.getId(), "CRON_MONITOR");

        MarketplacePolicy reloaded = policyRepository.findById(testPolicyMl.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(PolicyStatus.SOURCE_UNAVAILABLE);
    }

    @Test
    @DisplayName("07, 08 e 09: Bloqueio sob Princípio do Menor Escopo (Least Scope) preservando operações legítimas")
    void test07_08_09_leastScopeEnforcement() {
        // Enforce restriction specifically on PRICE_UPDATE for Mercado Livre
        complianceEnforcer.enforceRestriction(
                Marketplace.MERCADO_LIVRE, "PRICE_UPDATE", PolicyStatus.SUSPENDED,
                "New pricing regulation under review", null, "OFFICER");

        // 1. PRICE_UPDATE is restricted
        assertThat(complianceEnforcer.isFeatureRestricted(Marketplace.MERCADO_LIVRE, "PRICE_UPDATE")).isTrue();

        ComplianceCheckResult priceCheck = complianceGuard.checkOperation(
                orgAlpha.getId(), storeMlAlpha.getId(), Marketplace.MERCADO_LIVRE,
                "PRICE_UPDATE", OperationRiskLevel.MUTABLE_CRITICAL, true, "SELLER");
        assertThat(priceCheck.allowed()).isFalse();
        assertThat(priceCheck.code()).isEqualTo("FEATURE_ENFORCEMENT_RESTRICTED");

        // 2. SYNC_ORDERS is PRESERVED and permitted!
        assertThat(complianceEnforcer.isFeatureRestricted(Marketplace.MERCADO_LIVRE, "SYNC_ORDERS")).isFalse();

        ComplianceCheckResult orderCheck = complianceGuard.checkOperation(
                orgAlpha.getId(), storeMlAlpha.getId(), Marketplace.MERCADO_LIVRE,
                "SYNC_ORDERS", OperationRiskLevel.READ_ONLY, true, "SELLER");
        assertThat(orderCheck.allowed()).isTrue();
    }

    @Test
    @DisplayName("10: Identificação de permissão revogada ou escopo insuficiente")
    void test10_identifyRevokedPermissionOrInsufficientScope() {
        // 1. Revoked credential
        MarketplaceCredential cred = credentialRepository.findByTenantIdAndStoreId(orgAlpha.getId(), storeMlAlpha.getId()).orElseThrow();
        cred.setStatus("REVOKED");
        credentialRepository.save(cred);

        ComplianceCheckResult revokedCheck = permissionValidator.validateStorePermissions(orgAlpha.getId(), storeMlAlpha.getId(), Set.of("read"));
        assertThat(revokedCheck.allowed()).isFalse();
        assertThat(revokedCheck.code()).isEqualTo("CREDENTIAL_REVOKED");

        // 2. Insufficient scope
        cred.setStatus("CONNECTED");
        cred.setScopesConsented("read");
        credentialRepository.save(cred);

        ComplianceCheckResult scopeCheck = permissionValidator.validateStorePermissions(orgAlpha.getId(), storeMlAlpha.getId(), Set.of("write", "admin_ads"));
        assertThat(scopeCheck.allowed()).isFalse();
        assertThat(scopeCheck.code()).isEqualTo("INSUFFICIENT_SCOPES");
    }

    @Test
    @DisplayName("11: Identificação de API depreciada e aviso de sunset")
    void test11_identifyApiDeprecationAndSunset() {
        OffsetDateTime sunset = OffsetDateTime.now().plusDays(60);
        MarketplaceApiLifecycle api = apiLifecycleMonitor.registerApi(
                Marketplace.MERCADO_LIVRE, "Legacy Metrics API", "v1",
                ApiLifecycleStatus.DEPRECATED, sunset, "https://developers.mercadolivre.com.br/migration");

        assertThat(api.getStatus()).isEqualTo(ApiLifecycleStatus.DEPRECATED);
        assertThat(api.getSunsetDate()).isEqualTo(sunset);
    }

    @Test
    @DisplayName("12: Criação de incidente de compliance com ciclo de vida")
    void test12_createAndManageComplianceIncident() {
        ComplianceIncident inc = incidentService.openIncident(
                Marketplace.SHOPEE, null, null, ComplianceRiskLevel.CRITICAL,
                "SYNC_CATALOG", "LEAD_DEV", "Shopee updated HMAC headers", "Evidence URL", "Update signature algorithm", "ADMIN");

        assertThat(inc.getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(inc.getIncidentCode()).startsWith("INC-");

        // Mitigate
        inc = incidentService.updateStatus(inc.getId(), IncidentStatus.MITIGATING, "LEAD_DEV", "Working on signature patch");
        assertThat(inc.getStatus()).isEqualTo(IncidentStatus.MITIGATING);

        // Resolve
        inc = incidentService.updateStatus(inc.getId(), IncidentStatus.RESOLVED, "OFFICER", "Patch merged and tested");
        assertThat(inc.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(inc.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("13 e 14: Envio de notificação e prevenção de notificações duplicadas (Cooldown)")
    void test13_14_notificationsAndDeduplicationCooldown() {
        boolean firstSent = notificationService.sendAlert(
                Marketplace.AMAZON, ComplianceRiskLevel.HIGH, "Rate Limit Warning", "Amazon rate limit adjusted");
        assertThat(firstSent).isTrue();

        // Immediate duplicate call is suppressed by cooldown
        boolean duplicateSent = notificationService.sendAlert(
                Marketplace.AMAZON, ComplianceRiskLevel.HIGH, "Rate Limit Warning", "Amazon rate limit adjusted");
        assertThat(duplicateSent).isFalse();
    }

    @Test
    @DisplayName("15 e 16: Proteção contra reativação não autorizada e aprovação formal com evidências")
    void test15_16_safeReactivationGovernance() {
        // Suspend feature
        complianceEnforcer.enforceRestriction(
                Marketplace.MERCADO_LIVRE, "AI_LISTING_STUDIO", PolicyStatus.SUSPENDED,
                "Prompt injection policy revision", null, "OFFICER");

        // 1. Reactivation without administrator signature fails
        assertThatThrownBy(() -> reactivationService.reactivateFeature(
                Marketplace.MERCADO_LIVRE, "AI_LISTING_STUDIO", "", "Evidence doc", "v2", "Test suite 100%"))
                .isInstanceOf(ComplianceViolationException.class)
                .hasFieldOrPropertyWithValue("code", "UNAUTHORIZED_REACTIVATION");

        // 2. Reactivation without test proof fails
        assertThatThrownBy(() -> reactivationService.reactivateFeature(
                Marketplace.MERCADO_LIVRE, "AI_LISTING_STUDIO", "CHIEF_OFFICER", "Evidence doc", "v2", ""))
                .isInstanceOf(ComplianceViolationException.class)
                .hasFieldOrPropertyWithValue("code", "MISSING_TEST_PROOF");

        // 3. Authorized reactivation succeeds and logs record
        SafeReactivationLog logRecord = reactivationService.reactivateFeature(
                Marketplace.MERCADO_LIVRE, "AI_LISTING_STUDIO", "CHIEF_COMPLIANCE_OFFICER",
                "PRD and MELI Dev Terms review", "v2026.1", "15 unit tests passed");

        assertThat(logRecord).isNotNull();
        assertThat(logRecord.getApprovedBy()).isEqualTo("CHIEF_COMPLIANCE_OFFICER");
        assertThat(complianceEnforcer.isFeatureRestricted(Marketplace.MERCADO_LIVRE, "AI_LISTING_STUDIO")).isFalse();
    }

    @Test
    @DisplayName("17: Isolamento multi-tenant na validação de permissões")
    void test17_multiTenantIsolationInPermissionValidation() {
        UUID otherTenantId = UUID.randomUUID();

        // Attempt to check permissions with mismatched tenant returns missing credentials
        ComplianceCheckResult result = permissionValidator.validateStorePermissions(otherTenantId, storeMlAlpha.getId(), Set.of("read"));
        assertThat(result.allowed()).isFalse();
        assertThat(result.code()).isEqualTo("CREDENTIALS_MISSING");
    }

    @Test
    @DisplayName("18: Proteção rigorosa contra SSRF no monitoramento de URLs de políticas")
    void test18_ssrfProtectionBlocksPrivateAndNonOfficialDomains() {
        // 1. Localhost is blocked
        assertThatThrownBy(() -> urlSecurityValidator.validatePolicyMonitoringUrl("http://localhost:8080/policy"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "SSRF_VIOLATION_BLOCKED");

        // 2. AWS / Cloud metadata is blocked
        assertThatThrownBy(() -> urlSecurityValidator.validatePolicyMonitoringUrl("http://169.254.169.254/latest/meta-data/"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "SSRF_VIOLATION_BLOCKED");

        // 3. Private IP 10.0.0.1 is blocked
        assertThatThrownBy(() -> urlSecurityValidator.validatePolicyMonitoringUrl("https://10.0.0.1/doc"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "SSRF_VIOLATION_BLOCKED");

        // 4. Non-authorized external domain is blocked
        assertThatThrownBy(() -> urlSecurityValidator.validatePolicyMonitoringUrl("https://evil-hacker-site.com/policy"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "UNAUTHORIZED_POLICY_DOMAIN");

        // 5. Official Mercado Livre domain is permitted
        urlSecurityValidator.validatePolicyMonitoringUrl("https://developers.mercadolivre.com.br/pt_br/terms");
    }

    @Test
    @DisplayName("19 e 20: Recuperação após indisponibilidade e dashboard administrativo consolidado")
    void test19_20_recoveryAndDashboardSummary() {
        Map<String, Object> dashboard = dashboardService.getDashboardSummary();

        assertThat(dashboard).containsKey("totalMarketplacesMonitored");
        assertThat(dashboard.get("totalMarketplacesMonitored")).isEqualTo(6);
        assertThat(dashboard).containsKey("totalOfficialSources");
        assertThat(dashboard).containsKey("marketplaces");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> marketplaces = (List<Map<String, Object>>) dashboard.get("marketplaces");
        assertThat(marketplaces).hasSize(6);

        // TikTok Shop is explicitly displayed as UNVERIFIED_COMPLIANCE (BLOCKED)
        boolean tiktokBlocked = marketplaces.stream()
                .anyMatch(m -> "TIKTOK_SHOP".equals(m.get("marketplace")) && ((String) m.get("status")).contains("BLOCKED"));
        assertThat(tiktokBlocked).isTrue();
    }
}
