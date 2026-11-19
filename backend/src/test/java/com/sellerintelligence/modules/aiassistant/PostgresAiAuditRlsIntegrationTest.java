package com.sellerintelligence.modules.aiassistant;

import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.core.tenant.TenantDatabaseSession;
import com.sellerintelligence.modules.aiassistant.entity.AiUsageAuditRecord;
import com.sellerintelligence.modules.aiassistant.repository.AiUsageAuditRepository;
import com.sellerintelligence.modules.tenancy.Organization;
import com.sellerintelligence.modules.tenancy.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de integração PostgreSQL 17 real via Testcontainers validando Row-Level Security (RLS)
 * na tabela ai_usage_audits (P019-BE).
 */
@Testcontainers
@SpringBootTest
class PostgresAiAuditRlsIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withInitScript("init-testcontainers-db.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "seller_app");
        registry.add("spring.datasource.password", () -> "seller_app");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/migration-postgresql");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private AiUsageAuditRepository auditRepository;

    @Autowired
    private TenantDatabaseSession tenantDatabaseSession;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    private UUID tenantAlphaId;
    private UUID tenantBetaId;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        TenantContextHolder.clear();

        tenantAlphaId = UUID.randomUUID();
        tenantBetaId = UUID.randomUUID();

        transactionTemplate.execute(status -> {
            Organization orgAlpha = new Organization(
                    tenantAlphaId,
                    "Alpha AI Org",
                    "alpha-ai-" + tenantAlphaId.toString().substring(0, 8),
                    "ACTIVE",
                    Instant.now(),
                    Instant.now()
            );
            Organization orgBeta = new Organization(
                    tenantBetaId,
                    "Beta AI Org",
                    "beta-ai-" + tenantBetaId.toString().substring(0, 8),
                    "ACTIVE",
                    Instant.now(),
                    Instant.now()
            );
            organizationRepository.saveAll(List.of(orgAlpha, orgBeta));
            return null;
        });
    }

    @Test
    @DisplayName("RLS PostgreSQL: Tenant Alpha grava auditoria de IA e Tenant Beta é hermeticamente bloqueado de consultá-la")
    void shouldIsolateAiUsageAuditsBetweenTenantsUnderPostgreSqlRls() {
        UUID auditAlphaId = UUID.randomUUID();
        UUID auditBetaId = UUID.randomUUID();

        // 1. Tenant Alpha grava seu registro de auditoria
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            AiUsageAuditRecord auditAlpha = new AiUsageAuditRecord(
                    auditAlphaId,
                    tenantAlphaId,
                    UUID.randomUUID(),
                    "GEMINI",
                    "gemini-2.0-flash",
                    "LISTING_QUALITY_AUDIT",
                    "v1.0.0",
                    200,
                    100,
                    300,
                    150L,
                    new BigDecimal("0.000250"),
                    "search_listings",
                    false,
                    null,
                    "SUCCESS",
                    null,
                    null,
                    Instant.now()
            );
            auditRepository.save(auditAlpha);
            return null;
        });

        // 2. Tenant Beta grava seu próprio registro de auditoria
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            AiUsageAuditRecord auditBeta = new AiUsageAuditRecord(
                    auditBetaId,
                    tenantBetaId,
                    UUID.randomUUID(),
                    "OPENAI",
                    "gpt-4o-mini",
                    "FINANCIAL_PRICE_EXPLAINER",
                    "v1.0.0",
                    120,
                    60,
                    180,
                    90L,
                    new BigDecimal("0.000300"),
                    "calculate_margin",
                    false,
                    null,
                    "SUCCESS",
                    null,
                    null,
                    Instant.now()
            );
            auditRepository.save(auditBeta);
            return null;
        });

        // 3. Verificação com sessão de Tenant Alpha: visualiza APENAS auditAlphaId
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            Optional<AiUsageAuditRecord> foundAlpha = auditRepository.findById(auditAlphaId);
            assertThat(foundAlpha).isPresent();
            assertThat(foundAlpha.get().getTenantId()).isEqualTo(tenantAlphaId);
            assertThat(foundAlpha.get().getProvider()).isEqualTo("GEMINI");

            // Tentativa de consultar o registro do Tenant Beta retorna vazio (RLS ativo)
            Optional<AiUsageAuditRecord> foundBetaCrossTenant = auditRepository.findById(auditBetaId);
            assertThat(foundBetaCrossTenant).isEmpty();

            List<AiUsageAuditRecord> allForAlpha = auditRepository.findAll();
            assertThat(allForAlpha)
                    .extracting(AiUsageAuditRecord::getId)
                    .contains(auditAlphaId)
                    .doesNotContain(auditBetaId);

            return null;
        });

        // 4. Verificação com sessão de Tenant Beta: visualiza APENAS auditBetaId
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            Optional<AiUsageAuditRecord> foundBeta = auditRepository.findById(auditBetaId);
            assertThat(foundBeta).isPresent();
            assertThat(foundBeta.get().getTenantId()).isEqualTo(tenantBetaId);
            assertThat(foundBeta.get().getProvider()).isEqualTo("OPENAI");

            // Tentativa de consultar o registro do Tenant Alpha retorna vazio (RLS ativo)
            Optional<AiUsageAuditRecord> foundAlphaCrossTenant = auditRepository.findById(auditAlphaId);
            assertThat(foundAlphaCrossTenant).isEmpty();

            List<AiUsageAuditRecord> allForBeta = auditRepository.findAll();
            assertThat(allForBeta)
                    .extracting(AiUsageAuditRecord::getId)
                    .contains(auditBetaId)
                    .doesNotContain(auditAlphaId);

            return null;
        });
    }
}
