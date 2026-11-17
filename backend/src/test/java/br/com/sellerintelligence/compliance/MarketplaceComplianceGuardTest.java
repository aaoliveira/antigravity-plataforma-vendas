package br.com.sellerintelligence.compliance;

import br.com.sellerintelligence.audit.domain.AuditEvent;
import br.com.sellerintelligence.audit.domain.AuditEventRepository;
import br.com.sellerintelligence.audit.domain.AuditResult;
import br.com.sellerintelligence.compliance.domain.ComplianceCheckResult;
import br.com.sellerintelligence.compliance.domain.ComplianceStatus;
import br.com.sellerintelligence.compliance.domain.OperationRiskLevel;
import br.com.sellerintelligence.compliance.exception.ComplianceViolationException;
import br.com.sellerintelligence.compliance.service.MarketplaceComplianceGuard;
import br.com.sellerintelligence.config.TestSecurityConfig;
import br.com.sellerintelligence.connector.model.MarketplaceCredential;
import br.com.sellerintelligence.connector.model.MarketplaceCredentialRepository;
import br.com.sellerintelligence.connector.service.MarketplaceConnectionService;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Transactional
public class MarketplaceComplianceGuardTest {

    @Autowired
    private MarketplaceComplianceGuard complianceGuard;

    @Autowired
    private MarketplaceConnectionService connectionService;

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
    private Organization orgBeta;
    private User ownerAlpha;
    private User viewerAlpha;
    private User ownerBeta;
    private Company companyAlpha;
    private Company companyBeta;
    private Store storeAlpha;
    private Store storeBeta;
    private MarketplaceCredential credentialAlpha;

    @BeforeEach
    void setUp() {
        credentialRepository.deleteAll();
        storeRepository.deleteAll();
        companyRepository.deleteAll();
        membershipRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();
        auditEventRepository.deleteAll();

        // Organizations
        orgAlpha = organizationRepository.save(new Organization("Alpha Corp", "alpha-corp"));
        orgBeta = organizationRepository.save(new Organization("Beta Retail", "beta-retail"));

        // Users
        ownerAlpha = userRepository.save(new User("sub-alpha-owner", "alpha@test.com", "Alpha Owner"));
        viewerAlpha = userRepository.save(new User("sub-alpha-viewer", "viewer@test.com", "Alpha Viewer"));
        ownerBeta = userRepository.save(new User("sub-beta-owner", "beta@test.com", "Beta Owner"));

        // Memberships
        membershipRepository.save(new Membership(orgAlpha, ownerAlpha, Role.OWNER, MembershipStatus.ACTIVE));
        membershipRepository.save(new Membership(orgAlpha, viewerAlpha, Role.VIEWER, MembershipStatus.ACTIVE));
        membershipRepository.save(new Membership(orgBeta, ownerBeta, Role.OWNER, MembershipStatus.ACTIVE));

        // Companies
        companyAlpha = companyRepository.save(new Company(
                orgAlpha.getId(), "11.222.333/0001-81", "Alpha LTDA", "Alpha Fantasia", TaxRegime.SIMPLES_NACIONAL));
        companyBeta = companyRepository.save(new Company(
                orgBeta.getId(), "44.555.666/0001-92", "Beta LTDA", "Beta Fantasia", TaxRegime.LUCRO_PRESUMIDO));

        // Stores
        storeAlpha = new Store(orgAlpha.getId(), companyAlpha, Marketplace.MERCADO_LIVRE, "Loja ML Alpha");
        storeAlpha.setConnectionStatus(ConnectionStatus.CONNECTED);
        storeAlpha = storeRepository.save(storeAlpha);

        storeBeta = new Store(orgBeta.getId(), companyBeta, Marketplace.MERCADO_LIVRE, "Loja ML Beta");
        storeBeta.setConnectionStatus(ConnectionStatus.CONNECTED);
        storeBeta = storeRepository.save(storeBeta);

        // Active credential for Alpha
        credentialAlpha = new MarketplaceCredential(
                orgAlpha.getId(),
                storeAlpha,
                Marketplace.MERCADO_LIVRE,
                "enc_access_token_123",
                "enc_refresh_token_456",
                "MLB-ACCT-01",
                "read,write",
                OffsetDateTime.now().plusDays(30),
                OffsetDateTime.now().plusMonths(6),
                "production"
        );
        credentialAlpha.setStatus("CONNECTED");
        credentialAlpha = credentialRepository.save(credentialAlpha);

        // Reset circuit state for fresh test execution
        complianceGuard.resetCircuit(storeAlpha.getId());
        complianceGuard.resetCircuit(storeBeta.getId());
    }

    @Test
    @DisplayName("Cenário 1: Operação não autorizada é bloqueada sob política FAIL CLOSED")
    void scenario01_unauthorizedOperationIsBlocked_FailClosed() {
        UUID unknownStoreId = UUID.randomUUID();

        ComplianceCheckResult result = complianceGuard.checkOperation(
                orgAlpha.getId(), unknownStoreId, Marketplace.MERCADO_LIVRE,
                OperationRiskLevel.READ_ONLY, true, ownerAlpha.getId().toString());

        assertThat(result.allowed()).isFalse();
        assertThat(result.code()).isEqualTo("STORE_NOT_FOUND_OR_CROSS_TENANT");

        assertThatThrownBy(() -> complianceGuard.enforceOperation(
                orgAlpha.getId(), unknownStoreId, Marketplace.MERCADO_LIVRE,
                OperationRiskLevel.READ_ONLY, true, ownerAlpha.getId().toString()))
                .isInstanceOf(ComplianceViolationException.class)
                .hasFieldOrPropertyWithValue("code", "STORE_NOT_FOUND_OR_CROSS_TENANT");
    }

    @Test
    @DisplayName("Cenário 2: Token expirado impede operações no marketplace")
    void scenario02_expiredTokenBlocksOperations() {
        credentialAlpha.setExpiresAt(OffsetDateTime.now().minusHours(2));
        credentialRepository.save(credentialAlpha);

        ComplianceCheckResult result = complianceGuard.checkOperation(
                orgAlpha.getId(), storeAlpha.getId(), Marketplace.MERCADO_LIVRE,
                OperationRiskLevel.READ_ONLY, true, ownerAlpha.getId().toString());

        assertThat(result.allowed()).isFalse();
        assertThat(result.code()).isEqualTo("TOKEN_EXPIRED");
        assertThat(result.reason()).contains("expired");

        assertThatThrownBy(() -> complianceGuard.enforceOperation(
                orgAlpha.getId(), storeAlpha.getId(), Marketplace.MERCADO_LIVRE,
                OperationRiskLevel.READ_ONLY, true, ownerAlpha.getId().toString()))
                .isInstanceOf(ComplianceViolationException.class)
                .hasFieldOrPropertyWithValue("code", "TOKEN_EXPIRED");
    }

    @Test
    @DisplayName("Cenário 3: Token revogado impede novas operações")
    void scenario03_revokedTokenBlocksOperations() {
        credentialAlpha.setStatus("REVOKED");
        credentialRepository.save(credentialAlpha);

        ComplianceCheckResult result = complianceGuard.checkOperation(
                orgAlpha.getId(), storeAlpha.getId(), Marketplace.MERCADO_LIVRE,
                OperationRiskLevel.READ_ONLY, true, ownerAlpha.getId().toString());

        assertThat(result.allowed()).isFalse();
        assertThat(result.code()).isEqualTo("CREDENTIAL_REVOKED");

        assertThatThrownBy(() -> complianceGuard.enforceOperation(
                orgAlpha.getId(), storeAlpha.getId(), Marketplace.MERCADO_LIVRE,
                OperationRiskLevel.READ_ONLY, true, ownerAlpha.getId().toString()))
                .isInstanceOf(ComplianceViolationException.class)
                .hasFieldOrPropertyWithValue("code", "CREDENTIAL_REVOKED");
    }

    @Test
    @DisplayName("Cenário 4: HTTP 429 aciona tratamento de limite e cooldown temporário")
    void scenario04_http429TriggersRateLimitCooldownAndBlocksExecution() {
        complianceGuard.recordFailure(storeAlpha.getId(), 429);

        assertThat(complianceGuard.isRateLimited(storeAlpha.getId())).isTrue();

        ComplianceCheckResult result = complianceGuard.checkOperation(
                orgAlpha.getId(), storeAlpha.getId(), Marketplace.MERCADO_LIVRE,
                OperationRiskLevel.READ_ONLY, true, ownerAlpha.getId().toString());

        assertThat(result.allowed()).isFalse();
        assertThat(result.code()).isEqualTo("RATE_LIMIT_COOLDOWN_ACTIVE");
        assertThat(result.retryAfterSeconds()).isNotNull().isGreaterThan(0L);

        assertThatThrownBy(() -> complianceGuard.enforceOperation(
                orgAlpha.getId(), storeAlpha.getId(), Marketplace.MERCADO_LIVRE,
                OperationRiskLevel.READ_ONLY, true, ownerAlpha.getId().toString()))
                .isInstanceOf(ComplianceViolationException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("Cenário 5: Falhas consecutivas acionam circuit breaker")
    void scenario05_consecutiveFailuresTripCircuitBreaker() {
        // Record 5 consecutive HTTP 500 errors
        for (int i = 0; i < 5; i++) {
            complianceGuard.recordFailure(storeAlpha.getId(), 500);
        }

        assertThat(complianceGuard.isCircuitOpen(storeAlpha.getId())).isTrue();

        ComplianceCheckResult result = complianceGuard.checkOperation(
                orgAlpha.getId(), storeAlpha.getId(), Marketplace.MERCADO_LIVRE,
                OperationRiskLevel.READ_ONLY, true, ownerAlpha.getId().toString());

        assertThat(result.allowed()).isFalse();
        assertThat(result.code()).isEqualTo("CIRCUIT_BREAKER_OPEN");

        assertThatThrownBy(() -> complianceGuard.enforceOperation(
                orgAlpha.getId(), storeAlpha.getId(), Marketplace.MERCADO_LIVRE,
                OperationRiskLevel.READ_ONLY, true, ownerAlpha.getId().toString()))
                .isInstanceOf(ComplianceViolationException.class)
                .hasFieldOrPropertyWithValue("code", "CIRCUIT_BREAKER_OPEN");
    }

    @Test
    @DisplayName("Cenário 6: Operações bloqueadas não são repetidas indefinidamente")
    void scenario06_blockedOperationsDoNotRetryIndefinitely() {
        complianceGuard.recordFailure(storeAlpha.getId(), 500);
        complianceGuard.recordFailure(storeAlpha.getId(), 500);
        complianceGuard.recordFailure(storeAlpha.getId(), 500);
        complianceGuard.recordFailure(storeAlpha.getId(), 500);
        complianceGuard.recordFailure(storeAlpha.getId(), 500);

        // Multiple sequential checks are immediately blocked locally without making remote calls
        for (int i = 0; i < 3; i++) {
            ComplianceCheckResult result = complianceGuard.checkOperation(
                    orgAlpha.getId(), storeAlpha.getId(), Marketplace.MERCADO_LIVRE,
                    OperationRiskLevel.READ_ONLY, true, ownerAlpha.getId().toString());
            assertThat(result.allowed()).isFalse();
            assertThat(result.code()).isEqualTo("CIRCUIT_BREAKER_OPEN");
        }
    }

    @Test
    @DisplayName("Cenário 7: Credenciais e tokens não aparecem em logs ou mensagens de exceção")
    void scenario07_credentialsNeverLeakInExceptionsOrLogs() {
        String sensitiveToken = "Bearer ya29.a0AfH6SMA-super-secret-oauth-token-12345";
        ComplianceViolationException ex = new ComplianceViolationException(
                "TOKEN_LEAK_PREVENTION", "Failed token request with: " + sensitiveToken);

        assertThat(ex.getMessage()).doesNotContain("ya29.a0AfH6SMA");
        assertThat(ex.getMessage()).contains("[REDACTED]");
    }

    @Test
    @DisplayName("Cenário 8: Uma organização não acessa dados de outra (Isolamento Cross-Tenant / IDOR)")
    void scenario08_crossTenantIdorIsBlocked() {
        // Org Alpha tries to execute operation on store belonging to Org Beta
        ComplianceCheckResult result = complianceGuard.checkOperation(
                orgAlpha.getId(), storeBeta.getId(), Marketplace.MERCADO_LIVRE,
                OperationRiskLevel.READ_ONLY, true, ownerAlpha.getId().toString());

        assertThat(result.allowed()).isFalse();
        assertThat(result.code()).isEqualTo("STORE_NOT_FOUND_OR_CROSS_TENANT");

        assertThatThrownBy(() -> complianceGuard.enforceOperation(
                orgAlpha.getId(), storeBeta.getId(), Marketplace.MERCADO_LIVRE,
                OperationRiskLevel.READ_ONLY, true, ownerAlpha.getId().toString()))
                .isInstanceOf(ComplianceViolationException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Cenário 9: Usuário sem permissão não executa operações críticas (RBAC)")
    void scenario09_userWithoutProperPermissionCannotManageConnection() {
        // Viewer Alpha attempts to initiate OAuth
        assertThatThrownBy(() -> connectionService.initiateOAuthFlow(
                orgAlpha.getId(), storeAlpha.getId(), "https://app.sellerintelligence.com/callback", viewerAlpha))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("Insufficient permissions");
    }

    @Test
    @DisplayName("Cenário 10: IA não consegue ignorar o mecanismo de autorização")
    void scenario10_aiCannotBypassComplianceGuard() {
        // AI tool attempt without human approval on critical mutation
        ComplianceCheckResult result = complianceGuard.checkOperation(
                orgAlpha.getId(), storeAlpha.getId(), Marketplace.MERCADO_LIVRE,
                OperationRiskLevel.MUTABLE_CRITICAL, false, "AI_AGENT_COPILOT");

        assertThat(result.allowed()).isFalse();
        assertThat(result.code()).isEqualTo("HUMAN_APPROVAL_REQUIRED");

        assertThatThrownBy(() -> complianceGuard.enforceOperation(
                orgAlpha.getId(), storeAlpha.getId(), Marketplace.MERCADO_LIVRE,
                OperationRiskLevel.MUTABLE_CRITICAL, false, "AI_AGENT_COPILOT"))
                .isInstanceOf(ComplianceViolationException.class)
                .hasFieldOrPropertyWithValue("code", "HUMAN_APPROVAL_REQUIRED");
    }

    @Test
    @DisplayName("Cenário 11: Extensão não executa ações sem autorização e loja conectada")
    void scenario11_extensionCannotExecuteUnauthorizedActions() {
        // Extension attempts operation on disconnected store
        storeAlpha.setConnectionStatus(ConnectionStatus.DISCONNECTED);
        storeRepository.save(storeAlpha);

        ComplianceCheckResult result = complianceGuard.checkOperation(
                orgAlpha.getId(), storeAlpha.getId(), Marketplace.MERCADO_LIVRE,
                OperationRiskLevel.READ_ONLY, true, "CHROME_EXTENSION_USER");

        assertThat(result.allowed()).isFalse();
        assertThat(result.code()).isEqualTo("STORE_CONNECTION_INVALID");
    }

    @Test
    @DisplayName("Cenário 12: Operações críticas exigem aprovação humana mandatória")
    void scenario12_mutableCriticalOperationsRequireHumanApproval() {
        // 1. Without human approval -> BLOCKED
        ComplianceCheckResult unapproved = complianceGuard.checkOperation(
                orgAlpha.getId(), storeAlpha.getId(), Marketplace.MERCADO_LIVRE,
                OperationRiskLevel.MUTABLE_CRITICAL, false, ownerAlpha.getId().toString());

        assertThat(unapproved.allowed()).isFalse();
        assertThat(unapproved.code()).isEqualTo("HUMAN_APPROVAL_REQUIRED");

        // 2. With explicit human approval -> ALLOWED
        ComplianceCheckResult approved = complianceGuard.checkOperation(
                orgAlpha.getId(), storeAlpha.getId(), Marketplace.MERCADO_LIVRE,
                OperationRiskLevel.MUTABLE_CRITICAL, true, ownerAlpha.getId().toString());

        assertThat(approved.allowed()).isTrue();
        assertThat(approved.code()).isEqualTo("OPERATION_ALLOWED");
    }

    @Test
    @DisplayName("Cenário 13: Canais com conformidade não validada (UNVERIFIED_COMPLIANCE) permanecem categoricamente bloqueados")
    void scenario13_unverifiedMarketplacesRemainBlocked() {
        assertThat(complianceGuard.getComplianceStatus(Marketplace.TIKTOK_SHOP))
                .isEqualTo(ComplianceStatus.UNVERIFIED_COMPLIANCE);
        assertThat(complianceGuard.getComplianceStatus(Marketplace.MAGALU))
                .isEqualTo(ComplianceStatus.UNVERIFIED_COMPLIANCE);
        assertThat(complianceGuard.getComplianceStatus(Marketplace.SHEIN))
                .isEqualTo(ComplianceStatus.UNVERIFIED_COMPLIANCE);

        // Verification check fails closed
        ComplianceCheckResult result = complianceGuard.checkOperation(
                orgAlpha.getId(), null, Marketplace.TIKTOK_SHOP,
                OperationRiskLevel.READ_ONLY, true, ownerAlpha.getId().toString());

        assertThat(result.allowed()).isFalse();
        assertThat(result.code()).isEqualTo("UNVERIFIED_MARKETPLACE_BLOCKED");

        // Attempting to initiate OAuth for unverified marketplace is blocked
        Store tiktokStore = storeRepository.save(new Store(orgAlpha.getId(), companyAlpha, Marketplace.TIKTOK_SHOP, "Loja TikTok"));
        assertThatThrownBy(() -> connectionService.initiateOAuthFlow(
                orgAlpha.getId(), tiktokStore.getId(), "https://app.sellerintelligence.com/callback", ownerAlpha))
                .isInstanceOf(ComplianceViolationException.class)
                .hasFieldOrPropertyWithValue("code", "UNVERIFIED_MARKETPLACE_BLOCKED");
    }

    @Test
    @DisplayName("Cenário 14: Eventos de auditoria são registrados com ator, tenant e motivo")
    void scenario14_auditEventsAreProperlyRecordedWithActorAndReason() {
        // Trigger a compliance block
        complianceGuard.checkOperation(
                orgAlpha.getId(), storeAlpha.getId(), Marketplace.MERCADO_LIVRE,
                OperationRiskLevel.MUTABLE_CRITICAL, false, ownerAlpha.getId().toString());

        List<AuditEvent> events = auditEventRepository.findByTenantIdOrderByCreatedAtDesc(orgAlpha.getId());
        assertThat(events).isNotEmpty();

        boolean found = events.stream().anyMatch(e ->
                "COMPLIANCE_MUTABLE_ACTION_REJECTED".equals(e.getAction())
                        && e.getResult() == AuditResult.DENIED
                        && e.getActorId().equals(ownerAlpha.getId().toString()));

        assertThat(found).isTrue();
    }

    @Test
    @DisplayName("Cenário 15: Revogação da integração interrompe imediatamente novas operações")
    void scenario15_revocationImmediatelyHaltsNewOperations() {
        // Disconnect store Alpha
        connectionService.disconnectStore(orgAlpha.getId(), storeAlpha.getId(), ownerAlpha);

        Store disconnected = storeRepository.findById(storeAlpha.getId()).orElseThrow();
        assertThat(disconnected.getConnectionStatus()).isEqualTo(ConnectionStatus.DISCONNECTED);

        // Immediate subsequent call is blocked
        ComplianceCheckResult result = complianceGuard.checkOperation(
                orgAlpha.getId(), storeAlpha.getId(), Marketplace.MERCADO_LIVRE,
                OperationRiskLevel.READ_ONLY, true, ownerAlpha.getId().toString());

        assertThat(result.allowed()).isFalse();
        assertThat(result.code()).isEqualTo("STORE_CONNECTION_INVALID");
    }
}
