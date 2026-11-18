package com.sellerintelligence.modules.competitorresearch.comparison;

import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.core.tenant.TenantDatabaseSession;
import com.sellerintelligence.modules.competitorresearch.comparison.entity.ListingComparisonRecord;
import com.sellerintelligence.modules.competitorresearch.comparison.entity.ListingPriceHistoryRecord;
import com.sellerintelligence.modules.competitorresearch.comparison.repository.ListingComparisonRepository;
import com.sellerintelligence.modules.competitorresearch.comparison.repository.ListingPriceHistoryRepository;
import com.sellerintelligence.modules.tenancy.Organization;
import com.sellerintelligence.modules.tenancy.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Teste de integração PostgreSQL real via Testcontainers para validação mandatória de RLS
 * (Row-Level Security) nas tabelas de Histórico de Preços e Comparações de Listings (P011-BE).
 */
@Testcontainers
@SpringBootTest
class PostgresListingComparisonRlsIntegrationTest {

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
    private ListingPriceHistoryRepository priceHistoryRepository;

    @Autowired
    private ListingComparisonRepository comparisonRepository;

    @Autowired
    private TenantDatabaseSession tenantDatabaseSession;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
                    "Alpha Comparison Org",
                    "alpha-comp-" + tenantAlphaId.toString().substring(0, 8),
                    "ACTIVE",
                    Instant.now(),
                    Instant.now()
            );
            Organization orgBeta = new Organization(
                    tenantBetaId,
                    "Beta Comparison Org",
                    "beta-comp-" + tenantBetaId.toString().substring(0, 8),
                    "ACTIVE",
                    Instant.now(),
                    Instant.now()
            );
            organizationRepository.save(orgAlpha);
            organizationRepository.save(orgBeta);
            return null;
        });
    }

    @Test
    @DisplayName("RLS: Tentativa de leitura cross-tenant em listing_price_history é bloqueada pelo PostgreSQL")
    void rlsPreventsCrossTenantPriceHistoryReading() {
        UUID pointAlphaId = UUID.randomUUID();

        // 1. Grava histórico como Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            ListingPriceHistoryRecord recordAlpha = new ListingPriceHistoryRecord(
                    pointAlphaId,
                    tenantAlphaId,
                    "MERCADO_LIVRE",
                    "MLB-SECRET-ALPHA",
                    "Produto Confidencial Alpha",
                    new BigDecimal("199.9000"),
                    "BRL",
                    new BigDecimal("15.0000"),
                    Instant.now(),
                    Instant.now(),
                    "OFFICIAL",
                    "https://produto.mercadolivre.com.br/MLB-SECRET-ALPHA"
            );
            priceHistoryRepository.save(recordAlpha);
            return null;
        });

        // 2. Consulta como Tenant Beta: RLS deve ocultar o registro de Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            Optional<ListingPriceHistoryRecord> found = priceHistoryRepository.findById(pointAlphaId);
            assertThat(found).isEmpty();

            List<ListingPriceHistoryRecord> allBeta = priceHistoryRepository.findAllByTenantIdAndMarketplaceAndExternalListingIdOrderByObservedAtAsc(
                    tenantBetaId, "MERCADO_LIVRE", "MLB-SECRET-ALPHA"
            );
            assertThat(allBeta).isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("RLS: Tentativa de leitura cross-tenant em listing_comparisons é bloqueada pelo PostgreSQL")
    void rlsPreventsCrossTenantComparisonReading() {
        UUID comparisonAlphaId = UUID.randomUUID();

        // 1. Grava comparação como Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            ListingComparisonRecord compAlpha = new ListingComparisonRecord(
                    comparisonAlphaId,
                    tenantAlphaId,
                    "BASE-01",
                    "Item Base Alpha",
                    "Marca Alpha",
                    "Modelo X",
                    "7891112223334",
                    new BigDecimal("100.0000"),
                    null,
                    Instant.now(),
                    "MERCADO_LIVRE",
                    "TARGET-01",
                    "Item Concorrente",
                    "Marca Alpha",
                    "Modelo X",
                    "7891112223334",
                    new BigDecimal("95.0000"),
                    null,
                    Instant.now(),
                    "SHOPEE",
                    "Vendedor Concorrente",
                    "https://shopee.com.br/item-01",
                    new BigDecimal("0.9500"),
                    new BigDecimal("1.0000"),
                    new BigDecimal("1.0000"),
                    new BigDecimal("1.0000"),
                    new BigDecimal("0.8500"),
                    new BigDecimal("0.9000"),
                    true,
                    true,
                    "COMPATIBLE",
                    "[]",
                    true,
                    new BigDecimal("-5.0000"),
                    new BigDecimal("-5.0000"),
                    0L,
                    "Comparação legítima de Alpha"
            );
            comparisonRepository.save(compAlpha);
            return null;
        });

        // 2. Consulta como Tenant Beta: RLS deve ocultar a comparação de Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            Optional<ListingComparisonRecord> found = comparisonRepository.findById(comparisonAlphaId);
            assertThat(found).isEmpty();

            Optional<ListingComparisonRecord> byTenantAndId = comparisonRepository.findByIdAndTenantId(comparisonAlphaId, tenantBetaId);
            assertThat(byTenantAndId).isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("RLS: Tentativa de inserção com spoofing de tenant_id é rejeitada pelo PostgreSQL WITH CHECK")
    void rlsRejectsCrossTenantInsertionWithCheck() {
        assertThatThrownBy(() -> {
            transactionTemplate.execute(status -> {
                tenantDatabaseSession.setTenantSession(tenantBetaId);

                // Sessão configurada para Beta, mas tentando gravar com tenantAlphaId
                ListingPriceHistoryRecord forgedRecord = new ListingPriceHistoryRecord(
                        UUID.randomUUID(),
                        tenantAlphaId,
                        "MERCADO_LIVRE",
                        "MLB-SPOOF",
                        "Titulo Spoof",
                        new BigDecimal("99.00"),
                        "BRL",
                        null,
                        Instant.now(),
                        Instant.now(),
                        "OFFICIAL",
                        null
                );
                priceHistoryRepository.saveAndFlush(forgedRecord);
                return null;
            });
        }).isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("RLS: Sessão sem tenant configurado retorna zero registros nas tabelas protegidas")
    void rlsReturnsZeroRowsWhenTenantContextUnset() {
        UUID pointId = UUID.randomUUID();

        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);
            ListingPriceHistoryRecord r = new ListingPriceHistoryRecord(
                    pointId, tenantAlphaId, "MERCADO_LIVRE", "MLB-TEST-RLS", "Item Teste",
                    new BigDecimal("50.00"), "BRL", null, Instant.now(), Instant.now(), "OFFICIAL", null
            );
            priceHistoryRepository.save(r);
            return null;
        });

        transactionTemplate.execute(status -> {
            jdbcTemplate.execute("RESET app.current_tenant;");
            List<UUID> rows = jdbcTemplate.query(
                    "SELECT id FROM listing_price_history WHERE id = ?",
                    (rs, rowNum) -> (UUID) rs.getObject("id"),
                    pointId
            );
            assertThat(rows).isEmpty();
            return null;
        });
    }
}
