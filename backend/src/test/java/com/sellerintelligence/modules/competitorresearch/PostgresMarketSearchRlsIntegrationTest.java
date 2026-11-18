package com.sellerintelligence.modules.competitorresearch;

import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.core.tenant.TenantDatabaseSession;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchCacheRecord;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchCacheRepository;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchQueryRecord;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchQueryRepository;
import com.sellerintelligence.modules.tenancy.Organization;
import com.sellerintelligence.modules.tenancy.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de integração PostgreSQL real via Testcontainers para validação mandatória de RLS
 * (Row-Level Security) nas tabelas de Pesquisa de Mercado e Cache (P010-BE).
 */
@Testcontainers
@SpringBootTest
class PostgresMarketSearchRlsIntegrationTest {

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
    private MarketSearchQueryRepository queryRepository;

    @Autowired
    private MarketSearchCacheRepository cacheRepository;

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
                    "Alpha Search Org",
                    "alpha-search-" + tenantAlphaId.toString().substring(0, 8),
                    "ACTIVE",
                    Instant.now(),
                    Instant.now()
            );
            Organization orgBeta = new Organization(
                    tenantBetaId,
                    "Beta Search Org",
                    "beta-search-" + tenantBetaId.toString().substring(0, 8),
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
    @DisplayName("RLS: Tentativa de leitura cross-tenant em market_search_queries é bloqueada pelo PostgreSQL")
    void rlsPreventsCrossTenantQueryHistoryReading() {
        UUID queryAlphaId = UUID.randomUUID();

        // 1. Grava histórico como Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            MarketSearchQueryRecord queryAlpha = new MarketSearchQueryRecord(
                    queryAlphaId,
                    tenantAlphaId,
                    "Termo Confidencial Alpha",
                    "MERCADO_LIVRE",
                    "Eletrônicos",
                    new BigDecimal("100.0000"),
                    new BigDecimal("500.0000"),
                    false,
                    "POL-MLB-PUBLIC-01",
                    "SUPPORTED",
                    10,
                    150,
                    false,
                    Instant.now()
            );
            queryRepository.save(queryAlpha);
            return null;
        });

        // 2. Consulta como Tenant Beta: RLS deve ocultar o registro de Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            Optional<MarketSearchQueryRecord> found = queryRepository.findById(queryAlphaId);
            assertThat(found).isEmpty();

            List<MarketSearchQueryRecord> allBeta = queryRepository.findAll();
            assertThat(allBeta).noneMatch(q -> q.getQueryText().contains("Termo Confidencial Alpha"));
            return null;
        });

        // 3. Consulta como Tenant Alpha: registro visível
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            Optional<MarketSearchQueryRecord> found = queryRepository.findById(queryAlphaId);
            assertThat(found).isPresent();
            assertThat(found.get().getQueryText()).isEqualTo("Termo Confidencial Alpha");
            return null;
        });
    }

    @Test
    @DisplayName("RLS: Tentativa de leitura ou mutação cross-tenant em market_search_cache é bloqueada pelo PostgreSQL")
    void rlsPreventsCrossTenantCacheAccessAndMutation() {
        UUID cacheAlphaId = UUID.randomUUID();
        String cacheKey = "cache-key-secret-alpha";

        // 1. Grava cache como Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            MarketSearchCacheRecord cacheAlpha = new MarketSearchCacheRecord(
                    cacheAlphaId,
                    tenantAlphaId,
                    cacheKey,
                    "POL-MLB-PUBLIC-01",
                    "{\"items\":[],\"cached\":true}",
                    0,
                    Instant.now().plus(15, ChronoUnit.MINUTES),
                    Instant.now()
            );
            cacheRepository.save(cacheAlpha);
            return null;
        });

        // 2. Tenta ler e atualizar cache como Tenant Beta
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            // Leitura bloqueada pelo RLS
            Optional<MarketSearchCacheRecord> found = cacheRepository.findByTenantIdAndCacheKey(tenantBetaId, cacheKey);
            assertThat(found).isEmpty();

            Optional<MarketSearchCacheRecord> foundById = cacheRepository.findById(cacheAlphaId);
            assertThat(foundById).isEmpty();

            // Mutação bloqueada: UPDATE afeta 0 linhas
            int updated = jdbcTemplate.update(
                    "UPDATE market_search_cache SET results_payload_json = '{\"tampered\": true}' WHERE id = ?",
                    cacheAlphaId
            );
            assertThat(updated).isEqualTo(0);

            // DELETE afeta 0 linhas
            int deleted = jdbcTemplate.update(
                    "DELETE FROM market_search_cache WHERE id = ?",
                    cacheAlphaId
            );
            assertThat(deleted).isEqualTo(0);
            return null;
        });

        // 3. Verifica integridade inalterada como Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            Optional<MarketSearchCacheRecord> found = cacheRepository.findById(cacheAlphaId);
            assertThat(found).isPresent();
            assertThat(found.get().getResultsPayloadJson()).isEqualTo("{\"items\":[],\"cached\":true}");
            return null;
        });
    }
}
