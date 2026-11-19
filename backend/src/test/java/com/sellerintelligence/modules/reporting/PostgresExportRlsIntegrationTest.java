package com.sellerintelligence.modules.reporting;

import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.core.tenant.TenantDatabaseSession;
import com.sellerintelligence.modules.reporting.entity.ExportAuditLog;
import com.sellerintelligence.modules.reporting.entity.ExportJob;
import com.sellerintelligence.modules.reporting.model.ExportAuditAction;
import com.sellerintelligence.modules.reporting.model.ExportDataset;
import com.sellerintelligence.modules.reporting.model.ExportFileFormat;
import com.sellerintelligence.modules.reporting.repository.ExportAuditLogRepository;
import com.sellerintelligence.modules.reporting.repository.ExportJobRepository;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@DisplayName("P021-BE: PostgresExportRlsIntegrationTest - Isolamento Multi-Tenant com RLS no PostgreSQL 17")
class PostgresExportRlsIntegrationTest {

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
    private ExportJobRepository exportJobRepository;

    @Autowired
    private ExportAuditLogRepository exportAuditLogRepository;

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
                    "Alpha Export Org",
                    "alpha-exp-" + tenantAlphaId.toString().substring(0, 8),
                    "ACTIVE",
                    Instant.now(),
                    Instant.now()
            );
            Organization orgBeta = new Organization(
                    tenantBetaId,
                    "Beta Export Org",
                    "beta-exp-" + tenantBetaId.toString().substring(0, 8),
                    "ACTIVE",
                    Instant.now(),
                    Instant.now()
            );
            organizationRepository.saveAll(List.of(orgAlpha, orgBeta));
            return null;
        });
    }

    @Test
    @DisplayName("RLS PostgreSQL: Jobs de exportação e logs de auditoria do Tenant Alpha permanecem invisíveis para o Tenant Beta")
    void shouldIsolateExportJobsAndAuditLogsBetweenTenantsUnderPostgreSqlRls() {
        UUID jobAlphaId = UUID.randomUUID();
        UUID auditAlphaId = UUID.randomUUID();

        UUID jobBetaId = UUID.randomUUID();
        UUID auditBetaId = UUID.randomUUID();

        // 1. Tenant Alpha grava job e auditoria
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            ExportJob jobAlpha = new ExportJob(
                    jobAlphaId,
                    tenantAlphaId,
                    "operator-alpha",
                    "idemp-alpha",
                    ExportDataset.FINANCIAL_FACTS,
                    ExportFileFormat.CSV,
                    "{\"marketplace\":\"MERCADO_LIVRE\"}",
                    1000
            );
            exportJobRepository.save(jobAlpha);

            ExportAuditLog auditAlpha = new ExportAuditLog(
                    auditAlphaId,
                    tenantAlphaId,
                    jobAlphaId,
                    ExportAuditAction.EXPORT_REQUESTED,
                    "operator-alpha",
                    "192.168.1.10",
                    "Agent-Alpha",
                    "{\"action\":\"test-alpha\"}"
            );
            exportAuditLogRepository.save(auditAlpha);

            return null;
        });

        // 2. Tenant Beta grava seu próprio job e auditoria
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            ExportJob jobBeta = new ExportJob(
                    jobBetaId,
                    tenantBetaId,
                    "operator-beta",
                    "idemp-beta",
                    ExportDataset.CATALOG,
                    ExportFileFormat.XLSX,
                    "{\"marketplace\":\"SHOPEE\"}",
                    500
            );
            exportJobRepository.save(jobBeta);

            ExportAuditLog auditBeta = new ExportAuditLog(
                    auditBetaId,
                    tenantBetaId,
                    jobBetaId,
                    ExportAuditAction.EXPORT_REQUESTED,
                    "operator-beta",
                    "192.168.1.20",
                    "Agent-Beta",
                    "{\"action\":\"test-beta\"}"
            );
            exportAuditLogRepository.save(auditBeta);

            return null;
        });

        // 3. Verificação com sessão de Tenant Alpha: visualiza APENAS Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            Optional<ExportJob> foundJobAlpha = exportJobRepository.findById(jobAlphaId);
            assertThat(foundJobAlpha).isPresent();
            assertThat(foundJobAlpha.get().getDataset()).isEqualTo(ExportDataset.FINANCIAL_FACTS);

            Optional<ExportAuditLog> foundAuditAlpha = exportAuditLogRepository.findById(auditAlphaId);
            assertThat(foundAuditAlpha).isPresent();

            // Bloqueio por RLS das entidades do Tenant Beta
            Optional<ExportJob> crossJobBeta = exportJobRepository.findById(jobBetaId);
            assertThat(crossJobBeta).isEmpty();

            Optional<ExportAuditLog> crossAuditBeta = exportAuditLogRepository.findById(auditBetaId);
            assertThat(crossAuditBeta).isEmpty();

            List<ExportJob> allAlphaJobs = exportJobRepository.findAll();
            assertThat(allAlphaJobs)
                    .extracting(ExportJob::getId)
                    .contains(jobAlphaId)
                    .doesNotContain(jobBetaId);

            return null;
        });

        // 4. Verificação com sessão de Tenant Beta: visualiza APENAS Beta
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            Optional<ExportJob> foundJobBeta = exportJobRepository.findById(jobBetaId);
            assertThat(foundJobBeta).isPresent();
            assertThat(foundJobBeta.get().getDataset()).isEqualTo(ExportDataset.CATALOG);

            Optional<ExportAuditLog> foundAuditBeta = exportAuditLogRepository.findById(auditBetaId);
            assertThat(foundAuditBeta).isPresent();

            // Bloqueio por RLS das entidades do Tenant Alpha
            Optional<ExportJob> crossJobAlpha = exportJobRepository.findById(jobAlphaId);
            assertThat(crossJobAlpha).isEmpty();

            Optional<ExportAuditLog> crossAuditAlpha = exportAuditLogRepository.findById(auditAlphaId);
            assertThat(crossAuditAlpha).isEmpty();

            List<ExportJob> allBetaJobs = exportJobRepository.findAll();
            assertThat(allBetaJobs)
                    .extracting(ExportJob::getId)
                    .contains(jobBetaId)
                    .doesNotContain(jobAlphaId);

            return null;
        });
    }
}
