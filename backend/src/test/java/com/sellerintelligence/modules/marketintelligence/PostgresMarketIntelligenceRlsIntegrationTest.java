package com.sellerintelligence.modules.marketintelligence;

import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.core.tenant.TenantDatabaseSession;
import com.sellerintelligence.modules.catalog.Product;
import com.sellerintelligence.modules.catalog.ProductRepository;
import com.sellerintelligence.modules.marketintelligence.model.ItemCondition;
import com.sellerintelligence.modules.marketintelligence.model.MarketObservationSourceType;
import com.sellerintelligence.modules.marketintelligence.model.MatchStatus;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Teste de integração PostgreSQL real via Testcontainers para validação mandatória de RLS
 * (Row-Level Security) nas tabelas de Inteligência de Mercado e Matching (P009-BE).
 */
@Testcontainers
@SpringBootTest
class PostgresMarketIntelligenceRlsIntegrationTest {

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
    private ProductRepository productRepository;

    @Autowired
    private MarketObservationRepository observationRepository;

    @Autowired
    private ProductMatchRepository productMatchRepository;

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
                    "Alpha Market Org",
                    "alpha-mkt-" + tenantAlphaId.toString().substring(0, 8),
                    "ACTIVE",
                    Instant.now(),
                    Instant.now()
            );
            Organization orgBeta = new Organization(
                    tenantBetaId,
                    "Beta Market Org",
                    "beta-mkt-" + tenantBetaId.toString().substring(0, 8),
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
    @DisplayName("RLS: Tentativa de leitura cross-tenant em market_observations é bloqueada pelo PostgreSQL")
    void rlsPreventsCrossTenantMarketObservationsReading() {
        UUID obsAlphaId = UUID.randomUUID();

        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            MarketObservation obsAlpha = new MarketObservation(
                    obsAlphaId,
                    tenantAlphaId,
                    "MLB-OBS-ALPHA-01",
                    "MERCADO_LIVRE",
                    "Produto Secreto Alpha",
                    new BigDecimal("199.9000"),
                    "BRL",
                    ItemCondition.NEW,
                    "Loja Alpha",
                    MarketObservationSourceType.OBSERVED,
                    "https://produto.mercadolivre.com.br/MLB-OBS-ALPHA-01",
                    Instant.now(),
                    Instant.now(),
                    Instant.now().plus(7, ChronoUnit.DAYS),
                    "OBSERVER",
                    BigDecimal.ONE,
                    null,
                    null,
                    null,
                    "7890000000001",
                    "AlphaBrand",
                    null,
                    null,
                    null,
                    null,
                    null
            );
            observationRepository.save(obsAlpha);
            return null;
        });

        // Sessão do Tenant Beta tenta ler a observação do Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            List<MarketObservation> betaObservations = observationRepository.findAll();
            assertThat(betaObservations).isEmpty();

            List<String> rawTitles = jdbcTemplate.query(
                    "SELECT title FROM market_observations WHERE id = ?",
                    (rs, rowNum) -> rs.getString("title"),
                    obsAlphaId
            );
            assertThat(rawTitles).isEmpty();

            return null;
        });
    }

    @Test
    @DisplayName("RLS: Tentativa de leitura cross-tenant em product_matches é bloqueada pelo PostgreSQL")
    void rlsPreventsCrossTenantProductMatchesReading() {
        UUID matchAlphaId = UUID.randomUUID();

        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            Product product = productRepository.save(new Product(
                    UUID.randomUUID(),
                    tenantAlphaId,
                    "Produto Canônico Alpha",
                    "SKU-ALPHA-MATCH",
                    "AlphaBrand",
                    "Geral",
                    "ACTIVE"
            ));

            MarketObservation obs = observationRepository.save(new MarketObservation(
                    UUID.randomUUID(),
                    tenantAlphaId,
                    "MLB-MATCH-ALPHA",
                    "MERCADO_LIVRE",
                    "Anúncio Concorrente Alpha",
                    new BigDecimal("150.0000"),
                    "BRL",
                    ItemCondition.NEW,
                    "Vendedor X",
                    MarketObservationSourceType.OBSERVED,
                    "https://produto.mercadolivre.com.br/MLB-MATCH-ALPHA",
                    Instant.now(),
                    Instant.now(),
                    Instant.now().plus(7, ChronoUnit.DAYS),
                    "SYNC",
                    BigDecimal.ONE,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            ));

            ProductMatch match = new ProductMatch(
                    matchAlphaId,
                    tenantAlphaId,
                    product.getId(),
                    obs.getId(),
                    new BigDecimal("0.8500"),
                    null,
                    new BigDecimal("0.8500"),
                    null,
                    null,
                    MatchStatus.MATCHED,
                    "Match aprovado",
                    new BigDecimal("0.7500"),
                    Instant.now()
            );
            productMatchRepository.save(match);
            return null;
        });

        // Sessão Tenant Beta tenta ler
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            List<ProductMatch> betaMatches = productMatchRepository.findAll();
            assertThat(betaMatches).isEmpty();

            List<String> rawExplanations = jdbcTemplate.query(
                    "SELECT explanation FROM product_matches WHERE id = ?",
                    (rs, rowNum) -> rs.getString("explanation"),
                    matchAlphaId
            );
            assertThat(rawExplanations).isEmpty();

            return null;
        });
    }

    @Test
    @DisplayName("RLS: Tentativa de escrita com spoofing de tenant em market_observations é rejeitada pelo PostgreSQL")
    void rlsRejectsWriteSpoofingInMarketObservations() {
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            assertThatThrownBy(() -> jdbcTemplate.update(
                    """
                    INSERT INTO market_observations (
                        id, tenant_id, observed_listing_id, marketplace, title, price,
                        currency, item_condition, source_type, source_url, collected_at,
                        observed_at, valid_until, method, confidence, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """,
                    UUID.randomUUID(),
                    tenantBetaId, // Spoofing: sessão é Alpha, mas tenta gravar Beta
                    "MLB-SPOOF-OBS",
                    "MERCADO_LIVRE",
                    "Produto Spoofing",
                    new BigDecimal("100.0000"),
                    "BRL",
                    "NEW",
                    "OBSERVED",
                    "https://produto.mercadolivre.com.br/MLB-SPOOF-OBS",
                    "MANUAL",
                    new BigDecimal("1.0000")
            )).isInstanceOf(DataAccessException.class);

            return null;
        });
    }
}
