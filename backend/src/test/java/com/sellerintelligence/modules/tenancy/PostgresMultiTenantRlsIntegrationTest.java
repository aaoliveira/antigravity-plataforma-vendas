package com.sellerintelligence.modules.tenancy;

import com.sellerintelligence.core.tenant.TenantAccessDeniedException;
import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.core.tenant.TenantDatabaseSession;
import com.sellerintelligence.core.tenant.TenantStoragePathResolver;
import com.sellerintelligence.modules.audit.AuditEvent;
import com.sellerintelligence.modules.audit.AuditEventRepository;
import com.sellerintelligence.modules.audit.AuditService;
import com.sellerintelligence.modules.marketplaceconnection.MarketplaceConnection;
import com.sellerintelligence.modules.marketplaceconnection.MarketplaceConnectionRepository;
import com.sellerintelligence.modules.reporting.TenantDocument;
import com.sellerintelligence.modules.reporting.TenantDocumentRepository;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Teste de integração rigoroso com PostgreSQL real via Testcontainers.
 * Valida a Defesa em Profundidade com Row-Level Security (RLS), filtros server-side,
 * transações isoladas, reutilização segura de conexões no pool e auditoria sem segredos.
 */
@Testcontainers
@SpringBootTest
class PostgresMultiTenantRlsIntegrationTest {

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
    private MarketplaceConnectionRepository connectionRepository;

    @Autowired
    private TenantDocumentRepository documentRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private AuditService auditService;

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

        // Cria organizações pré-requisito para as FKs em transação isolada
        transactionTemplate.execute(status -> {
            Organization orgAlpha = new Organization(
                    tenantAlphaId,
                    "Tenant Alpha Corp",
                    "alpha-" + tenantAlphaId.toString().substring(0, 8),
                    "ACTIVE",
                    Instant.now(),
                    Instant.now()
            );
            Organization orgBeta = new Organization(
                    tenantBetaId,
                    "Tenant Beta Ltda",
                    "beta-" + tenantBetaId.toString().substring(0, 8),
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
    @DisplayName("Cenário 1: Tentativa de leitura cross-tenant deve ser categoricamente bloqueada por RLS")
    void testCrossTenantReadDeniedByRls() {
        UUID alphaConnId = UUID.randomUUID();

        // 1. Tenant Alpha cadastra uma conexão dentro de sua transação e sessão RLS
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            MarketplaceConnection conn = new MarketplaceConnection(
                    alphaConnId,
                    tenantAlphaId,
                    "MERCADO_LIVRE",
                    "MLB12345678",
                    "Loja Alpha Oficial",
                    "enc_token_alpha",
                    "enc_refresh_alpha",
                    "ACTIVE"
            );
            connectionRepository.save(conn);
            return null;
        });

        // 2. Tenant Beta tenta ler o registro do Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            // Leitura via repositório JPA por ID direto
            Optional<MarketplaceConnection> foundById = connectionRepository.findById(alphaConnId);
            assertThat(foundById).isEmpty();

            // Leitura forçando filtro de tenant divergente
            Optional<MarketplaceConnection> foundByFilter = connectionRepository.findByTenantIdAndId(tenantAlphaId, alphaConnId);
            assertThat(foundByFilter).isEmpty();

            // Leitura direta via SQL nativo
            List<String> rawQuery = jdbcTemplate.query(
                    "SELECT account_name FROM marketplace_connections WHERE id = ?",
                    (rs, rowNum) -> rs.getString("account_name"),
                    alphaConnId
            );
            assertThat(rawQuery).isEmpty();

            return null;
        });
    }

    @Test
    @DisplayName("Cenário 2: Tentativa de criação cross-tenant deve ser bloqueada por RLS WITH CHECK")
    void testCrossTenantCreateDeniedByRls() {
        UUID fakeConnId = UUID.randomUUID();

        // Tenant Beta tenta forjar inserção com o tenant_id do Tenant Alpha
        assertThatThrownBy(() -> {
            transactionTemplate.execute(status -> {
                tenantDatabaseSession.setTenantSession(tenantBetaId);

                MarketplaceConnection forgedConn = new MarketplaceConnection(
                        fakeConnId,
                        tenantAlphaId, // Tentativa de spoofing
                        "SHOPEE",
                        "SHP999999",
                        "Loja Invasora",
                        "enc_token",
                        "enc_refresh",
                        "ACTIVE"
                );

                connectionRepository.saveAndFlush(forgedConn);
                return null;
            });
        }).isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("Cenário 3: Tentativa de alteração cross-tenant deve afetar 0 linhas e manter integridade")
    void testCrossTenantUpdateDeniedByRls() {
        UUID alphaConnId = UUID.randomUUID();

        // Cria conexão legítima no Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);
            MarketplaceConnection conn = new MarketplaceConnection(
                    alphaConnId,
                    tenantAlphaId,
                    "MERCADO_LIVRE",
                    "MLB123",
                    "Alpha Original",
                    "enc_token",
                    "enc_refresh",
                    "ACTIVE"
            );
            connectionRepository.save(conn);
            return null;
        });

        // Tenant Beta tenta executar UPDATE no registro do Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            int rowsUpdated = jdbcTemplate.update(
                    "UPDATE marketplace_connections SET account_name = 'Hacked by Beta' WHERE id = ?",
                    alphaConnId
            );
            assertThat(rowsUpdated).isEqualTo(0);
            return null;
        });

        // Verifica que no Tenant Alpha o dado permanece intacto
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);
            MarketplaceConnection conn = connectionRepository.findById(alphaConnId).orElseThrow();
            assertThat(conn.getAccountName()).isEqualTo("Alpha Original");
            return null;
        });
    }

    @Test
    @DisplayName("Cenário 4: Tentativa de exclusão cross-tenant deve afetar 0 linhas e manter arquivo íntegro")
    void testCrossTenantDeleteDeniedByRls() {
        UUID alphaDocId = UUID.randomUUID();

        // Cria documento no Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);
            TenantDocument doc = new TenantDocument(
                    alphaDocId,
                    tenantAlphaId,
                    "relatorio_financeiro.pdf",
                    "/tenants/" + tenantAlphaId + "/reports/relatorio_financeiro.pdf",
                    10240L,
                    "application/pdf",
                    "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
            );
            documentRepository.save(doc);
            return null;
        });

        // Tenant Beta tenta deletar o documento do Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            int rowsDeleted = jdbcTemplate.update(
                    "DELETE FROM tenant_documents WHERE id = ?",
                    alphaDocId
            );
            assertThat(rowsDeleted).isEqualTo(0);
            return null;
        });

        // Verifica que no Tenant Alpha o documento continua existindo
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);
            boolean exists = documentRepository.existsById(alphaDocId);
            assertThat(exists).isTrue();
            return null;
        });
    }

    @Test
    @DisplayName("Cenário 5: Busca e listagem devem retornar exclusivamente registros do tenant solicitante")
    void testCrossTenantSearchQueryReturnsOnlyOwnTenant() {
        // Insere 2 documentos no Tenant Alpha e 3 no Tenant Beta
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);
            for (int i = 1; i <= 2; i++) {
                documentRepository.save(new TenantDocument(
                        UUID.randomUUID(),
                        tenantAlphaId,
                        "doc_alpha_" + i + ".csv",
                        "/tenants/" + tenantAlphaId + "/docs/doc_" + i + ".csv",
                        1000L,
                        "text/csv",
                        "hash_alpha_" + i
                ));
            }
            return null;
        });

        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);
            for (int i = 1; i <= 3; i++) {
                documentRepository.save(new TenantDocument(
                        UUID.randomUUID(),
                        tenantBetaId,
                        "doc_beta_" + i + ".csv",
                        "/tenants/" + tenantBetaId + "/docs/doc_" + i + ".csv",
                        2000L,
                        "text/csv",
                        "hash_beta_" + i
                ));
            }
            return null;
        });

        // Valida que Tenant Alpha vê exatamente 2 documentos (nenhum de Beta)
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);
            List<TenantDocument> alphaDocs = documentRepository.findAll();
            assertThat(alphaDocs).hasSize(2);
            assertThat(alphaDocs).allMatch(doc -> doc.getTenantId().equals(tenantAlphaId));
            return null;
        });

        // Valida que Tenant Beta vê exatamente 3 documentos (nenhum de Alpha)
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);
            List<TenantDocument> betaDocs = documentRepository.findAll();
            assertThat(betaDocs).hasSize(3);
            assertThat(betaDocs).allMatch(doc -> doc.getTenantId().equals(tenantBetaId));
            return null;
        });
    }

    @Test
    @DisplayName("Cenário 6: Isolamento de storage deve barrar tentativas de acesso a caminhos de outro tenant")
    void testCrossTenantStoragePathIsolation() {
        String alphaPath = TenantStoragePathResolver.resolvePath(tenantAlphaId, "exports", "anuncios_export.xlsx");

        // Tenant Beta tenta acessar arquivo de Alpha
        assertThatThrownBy(() -> {
            TenantStoragePathResolver.validateAccess(tenantBetaId, alphaPath);
        }).isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("Acesso negado a arquivo de outro tenant");
    }

    @Test
    @DisplayName("Cenário 7: Reutilização de conexão no pool não deve reter contexto de transação anterior")
    void testConnectionPoolCleanReuseWithoutTenantContext() {
        UUID connId = UUID.randomUUID();

        // Transação 1: Tenant Alpha com SET LOCAL
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);
            connectionRepository.save(new MarketplaceConnection(
                    connId,
                    tenantAlphaId,
                    "SHOPEE",
                    "SHP123",
                    "Alpha Shopee",
                    "token",
                    "refresh",
                    "ACTIVE"
            ));
            return null;
        });

        // Transação 2: Conexão nova sem tenant configurado
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.resetTenantSession();

            String currentTenant = tenantDatabaseSession.getCurrentTenantSession();
            assertThat(currentTenant == null || currentTenant.isBlank()).isTrue();

            // Sob RLS sem tenant configurado, nenhuma linha deve ser visível
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM marketplace_connections",
                    Integer.class
            );
            assertThat(count).isEqualTo(0);

            return null;
        });
    }

    @Test
    @DisplayName("Cenário 8: Trilha de auditoria deve sanitizar e substituir senhas e segredos por [REDACTED]")
    void testAuditLogSanitizesSecretsInDatabase() {
        String sensitiveDetails = "{\"action\": \"OAUTH_CALLBACK\", \"access_token\": \"secret_access_xyz\", \"password\": \"my_plain_pass\", \"code\": \"auth_code_123\"}";

        auditService.publish(
                "TOKEN_REFRESH",
                UUID.randomUUID(),
                "seller@corp.com",
                tenantAlphaId,
                "192.168.1.100",
                "Mozilla/5.0",
                "corr-" + UUID.randomUUID(),
                sensitiveDetails
        );

        List<AuditEvent> events = auditEventRepository.findByTenantId(tenantAlphaId);
        assertThat(events).isNotEmpty();

        AuditEvent latest = events.get(events.size() - 1);
        assertThat(latest.getDetails()).doesNotContain("secret_access_xyz");
        assertThat(latest.getDetails()).doesNotContain("my_plain_pass");
        assertThat(latest.getDetails()).contains("\"access_token\": \"[REDACTED]\"");
        assertThat(latest.getDetails()).contains("\"password\": \"[REDACTED]\"");
    }
}
