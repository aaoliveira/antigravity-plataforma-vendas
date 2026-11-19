package com.sellerintelligence.modules.listinganalytics;

import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.core.tenant.TenantDatabaseSession;
import com.sellerintelligence.modules.catalog.SellerAccount;
import com.sellerintelligence.modules.catalog.SellerAccountRepository;
import com.sellerintelligence.modules.listinganalytics.entity.AccountMetricRecord;
import com.sellerintelligence.modules.listinganalytics.model.MetricAvailabilityStatus;
import com.sellerintelligence.modules.listinganalytics.model.MetricGranularity;
import com.sellerintelligence.modules.listinganalytics.model.MetricProvenance;
import com.sellerintelligence.modules.listinganalytics.model.MetricScope;
import com.sellerintelligence.modules.listinganalytics.model.MetricUnit;
import com.sellerintelligence.modules.listinganalytics.repository.AccountMetricRepository;
import com.sellerintelligence.modules.tenancy.Organization;
import com.sellerintelligence.modules.tenancy.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Teste de integração PostgreSQL 17 real via Testcontainers validando Row-Level Security (RLS)
 * na tabela connected_account_metrics (P018-BE).
 */
@Testcontainers
@SpringBootTest
class PostgresConnectedAccountMetricsRlsIntegrationTest {

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
    private SellerAccountRepository sellerAccountRepository;

    @Autowired
    private AccountMetricRepository metricRepository;

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
                    "Alpha Metrics Org",
                    "alpha-met-" + tenantAlphaId.toString().substring(0, 8),
                    "ACTIVE",
                    Instant.now(),
                    Instant.now()
            );
            Organization orgBeta = new Organization(
                    tenantBetaId,
                    "Beta Metrics Org",
                    "beta-met-" + tenantBetaId.toString().substring(0, 8),
                    "ACTIVE",
                    Instant.now(),
                    Instant.now()
            );
            organizationRepository.saveAll(List.of(orgAlpha, orgBeta));
            return null;
        });
    }

    @Test
    @DisplayName("RLS PostgreSQL: Tenant Alpha insere métrica e Tenant Beta não consegue visualizá-la")
    void shouldIsolateConnectedAccountMetricsBetweenTenantsUnderPostgreSqlRls() {
        UUID accountAlphaId = UUID.randomUUID();
        UUID metricId = UUID.randomUUID();
        Instant now = Instant.now().truncatedTo(ChronoUnit.DAYS);

        // 1. Tenant Alpha setup
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            SellerAccount account = new SellerAccount(
                    accountAlphaId,
                    tenantAlphaId,
                    "MERCADO_LIVRE",
                    "MLB-ACC-ALPHA",
                    "Loja Alpha",
                    "ACTIVE"
            );
            sellerAccountRepository.save(account);

            AccountMetricRecord metric = new AccountMetricRecord(
                    metricId,
                    tenantAlphaId,
                    accountAlphaId,
                    "MERCADO_LIVRE",
                    null,
                    "MLB-ITEM-ALPHA",
                    "MLB-ITEM-ALPHA",
                    MetricScope.LISTING,
                    MetricGranularity.DAILY,
                    now,
                    now.plus(1, ChronoUnit.DAYS),
                    "VISITS",
                    new BigDecimal("1500"),
                    MetricUnit.COUNT,
                    MetricProvenance.OFFICIAL_API,
                    null,
                    null,
                    null,
                    null,
                    null,
                    MetricAvailabilityStatus.AVAILABLE,
                    null,
                    "/items/visits",
                    Instant.now(),
                    null,
                    Instant.now(),
                    Instant.now()
            );
            metricRepository.save(metric);
            return null;
        });

        // 2. Consulta como Tenant Alpha -> deve encontrar
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            Optional<AccountMetricRecord> found = metricRepository.findByIdAndTenantId(metricId, tenantAlphaId);
            assertThat(found).isPresent();
            assertThat(found.get().getMetricValue()).isEqualByComparingTo("1500");
            return null;
        });

        // 3. Consulta como Tenant Beta -> isolamento total RLS (não encontra)
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            Optional<AccountMetricRecord> crossTenantFound = metricRepository.findByIdAndTenantId(metricId, tenantBetaId);
            assertThat(crossTenantFound).isEmpty();

            List<AccountMetricRecord> betaList = metricRepository.findAllByTenantIdAndSellerAccountId(tenantBetaId, accountAlphaId);
            assertThat(betaList).isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("RLS PostgreSQL: Tentativa de inserção cross-tenant é bloqueada com CHECK violation")
    void shouldBlockCrossTenantInsertionUnderPostgreSqlRls() {
        UUID accountBetaId = UUID.randomUUID();
        Instant now = Instant.now().truncatedTo(ChronoUnit.DAYS);

        // Prepara conta para Beta
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            SellerAccount account = new SellerAccount(
                    accountBetaId,
                    tenantBetaId,
                    "SHOPEE",
                    "SHOP-ACC-BETA",
                    "Loja Beta",
                    "ACTIVE"
            );
            sellerAccountRepository.save(account);
            return null;
        });

        // Sessão é Tenant Alpha, mas tenta gravar registro com tenantBetaId
        assertThatThrownBy(() -> transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            AccountMetricRecord rogueMetric = new AccountMetricRecord(
                    UUID.randomUUID(),
                    tenantBetaId, // Rogue cross-tenant id
                    accountBetaId,
                    "SHOPEE",
                    null,
                    null,
                    "__ACCOUNT__",
                    MetricScope.ACCOUNT,
                    MetricGranularity.DAILY,
                    now,
                    now.plus(1, ChronoUnit.DAYS),
                    "ORDERS_COUNT",
                    new BigDecimal("10"),
                    MetricUnit.COUNT,
                    MetricProvenance.OFFICIAL_API,
                    null,
                    null,
                    null,
                    null,
                    null,
                    MetricAvailabilityStatus.AVAILABLE,
                    null,
                    "v2.order",
                    Instant.now(),
                    null,
                    Instant.now(),
                    Instant.now()
            );
            metricRepository.save(rogueMetric);
            metricRepository.flush();
            return null;
        })).isInstanceOf(DataAccessException.class);
    }
}
