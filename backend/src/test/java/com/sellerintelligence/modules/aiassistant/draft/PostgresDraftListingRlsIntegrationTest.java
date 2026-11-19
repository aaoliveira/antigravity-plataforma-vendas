package com.sellerintelligence.modules.aiassistant.draft;

import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.core.tenant.TenantDatabaseSession;
import com.sellerintelligence.modules.aiassistant.draft.entity.DraftListing;
import com.sellerintelligence.modules.aiassistant.draft.entity.DraftListingVersion;
import com.sellerintelligence.modules.aiassistant.draft.repository.DraftListingRepository;
import com.sellerintelligence.modules.aiassistant.draft.repository.DraftListingVersionRepository;
import com.sellerintelligence.modules.tenancy.Organization;
import com.sellerintelligence.modules.tenancy.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
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

/**
 * Teste de integração PostgreSQL 17 real via Testcontainers validando Row-Level Security (RLS)
 * nas tabelas draft_listings e draft_listing_versions (P020-BE).
 */
@Testcontainers
@SpringBootTest
@DisplayName("PostgresDraftListingRlsIntegrationTest - Isolamento Multi-Tenant com RLS no PostgreSQL 17 (P020-BE)")
class PostgresDraftListingRlsIntegrationTest {

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
    private DraftListingRepository draftListingRepository;

    @Autowired
    private DraftListingVersionRepository draftVersionRepository;

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
                    "Alpha Drafts Org",
                    "alpha-drafts-" + tenantAlphaId.toString().substring(0, 8),
                    "ACTIVE",
                    Instant.now(),
                    Instant.now()
            );
            Organization orgBeta = new Organization(
                    tenantBetaId,
                    "Beta Drafts Org",
                    "beta-drafts-" + tenantBetaId.toString().substring(0, 8),
                    "ACTIVE",
                    Instant.now(),
                    Instant.now()
            );
            organizationRepository.saveAll(List.of(orgAlpha, orgBeta));
            return null;
        });
    }

    @Test
    @DisplayName("RLS PostgreSQL: Rascunhos e versões do Tenant Alpha permanecem hermeticamente invisíveis para o Tenant Beta")
    void shouldIsolateDraftListingsAndVersionsBetweenTenantsUnderPostgreSqlRls() {
        UUID draftAlphaId = UUID.randomUUID();
        UUID versionAlphaId = UUID.randomUUID();

        UUID draftBetaId = UUID.randomUUID();
        UUID versionBetaId = UUID.randomUUID();

        // 1. Tenant Alpha grava rascunho e versão
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            DraftListing draftAlpha = new DraftListing(
                    draftAlphaId,
                    tenantAlphaId,
                    null,
                    null,
                    "MERCADO_LIVRE",
                    1,
                    "DRAFT"
            );
            draftListingRepository.save(draftAlpha);

            DraftListingVersion versionAlpha = new DraftListingVersion(
                    versionAlphaId,
                    tenantAlphaId,
                    draftAlphaId,
                    1,
                    "LISTING_DRAFT_GENERATION",
                    "v1.0.0",
                    "mock-deterministic-v1",
                    "MOCK_PROVIDER",
                    "INFORMATIVE_OBJECTIVE",
                    "MERCADO_LIVRE",
                    "{\"brand\":\"Tramontina\"}",
                    "Panela Tramontina Solar Aço Inox",
                    "Descrição oficial Alpha",
                    null,
                    null,
                    "{\"brand\":\"Tramontina\"}",
                    null,
                    32,
                    23,
                    true,
                    false,
                    null
            );
            draftVersionRepository.save(versionAlpha);

            return null;
        });

        // 2. Tenant Beta grava seu próprio rascunho e versão
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            DraftListing draftBeta = new DraftListing(
                    draftBetaId,
                    tenantBetaId,
                    null,
                    null,
                    "SHOPEE",
                    1,
                    "DRAFT"
            );
            draftListingRepository.save(draftBeta);

            DraftListingVersion versionBeta = new DraftListingVersion(
                    versionBetaId,
                    tenantBetaId,
                    draftBetaId,
                    1,
                    "LISTING_DRAFT_GENERATION",
                    "v1.0.0",
                    "mock-deterministic-v1",
                    "MOCK_PROVIDER",
                    "COMMERCIAL_POLITE",
                    "SHOPEE",
                    "{\"brand\":\"Baseus\"}",
                    "Carregador Baseus 65W GaN5 USB-C",
                    "Descrição oficial Beta",
                    null,
                    null,
                    "{\"brand\":\"Baseus\"}",
                    null,
                    33,
                    22,
                    true,
                    false,
                    null
            );
            draftVersionRepository.save(versionBeta);

            return null;
        });

        // 3. Verificação com sessão de Tenant Alpha: visualiza APENAS dados do Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            Optional<DraftListing> foundDraftAlpha = draftListingRepository.findById(draftAlphaId);
            assertThat(foundDraftAlpha).isPresent();
            assertThat(foundDraftAlpha.get().getTargetMarketplace()).isEqualTo("MERCADO_LIVRE");

            Optional<DraftListingVersion> foundVersionAlpha = draftVersionRepository.findById(versionAlphaId);
            assertThat(foundVersionAlpha).isPresent();
            assertThat(foundVersionAlpha.get().getGeneratedTitle()).contains("Tramontina");

            // Tentativas de consultar registros de Beta retornam vazio devido ao RLS
            Optional<DraftListing> foundDraftBetaCross = draftListingRepository.findById(draftBetaId);
            assertThat(foundDraftBetaCross).isEmpty();

            Optional<DraftListingVersion> foundVersionBetaCross = draftVersionRepository.findById(versionBetaId);
            assertThat(foundVersionBetaCross).isEmpty();

            List<DraftListing> allAlphaDrafts = draftListingRepository.findAll();
            assertThat(allAlphaDrafts)
                    .extracting(DraftListing::getId)
                    .contains(draftAlphaId)
                    .doesNotContain(draftBetaId);

            return null;
        });

        // 4. Verificação com sessão de Tenant Beta: visualiza APENAS dados do Beta
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            Optional<DraftListing> foundDraftBeta = draftListingRepository.findById(draftBetaId);
            assertThat(foundDraftBeta).isPresent();
            assertThat(foundDraftBeta.get().getTargetMarketplace()).isEqualTo("SHOPEE");

            Optional<DraftListingVersion> foundVersionBeta = draftVersionRepository.findById(versionBetaId);
            assertThat(foundVersionBeta).isPresent();
            assertThat(foundVersionBeta.get().getGeneratedTitle()).contains("Baseus");

            // Tentativas de consultar registros de Alpha retornam vazio devido ao RLS
            Optional<DraftListing> foundDraftAlphaCross = draftListingRepository.findById(draftAlphaId);
            assertThat(foundDraftAlphaCross).isEmpty();

            Optional<DraftListingVersion> foundVersionAlphaCross = draftVersionRepository.findById(versionAlphaId);
            assertThat(foundVersionAlphaCross).isEmpty();

            List<DraftListing> allBetaDrafts = draftListingRepository.findAll();
            assertThat(allBetaDrafts)
                    .extracting(DraftListing::getId)
                    .contains(draftBetaId)
                    .doesNotContain(draftAlphaId);

            return null;
        });
    }
}
