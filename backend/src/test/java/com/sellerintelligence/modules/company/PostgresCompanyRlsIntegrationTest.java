package com.sellerintelligence.modules.company;

import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.core.tenant.TenantDatabaseSession;
import com.sellerintelligence.modules.catalog.SellerAccount;
import com.sellerintelligence.modules.catalog.SellerAccountRepository;
import com.sellerintelligence.modules.company.entity.CompanyAccountLinkRecord;
import com.sellerintelligence.modules.company.entity.CompanyRecord;
import com.sellerintelligence.modules.company.repository.CompanyAccountLinkRepository;
import com.sellerintelligence.modules.company.repository.CompanyRepository;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Teste de integração PostgreSQL real via Testcontainers para validação mandatória de RLS
 * (Row-Level Security) nas tabelas companies e company_seller_account_links (P016-BE).
 */
@Testcontainers
@SpringBootTest
class PostgresCompanyRlsIntegrationTest {

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
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyAccountLinkRepository linkRepository;

    @Autowired
    private SellerAccountRepository sellerAccountRepository;

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
                    "Alpha Company Org",
                    "alpha-comp-" + tenantAlphaId.toString().substring(0, 8),
                    "ACTIVE",
                    Instant.now(),
                    Instant.now()
            );
            Organization orgBeta = new Organization(
                    tenantBetaId,
                    "Beta Company Org",
                    "beta-comp-" + tenantBetaId.toString().substring(0, 8),
                    "ACTIVE",
                    Instant.now(),
                    Instant.now()
            );
            organizationRepository.saveAll(List.of(orgAlpha, orgBeta));
            return null;
        });
    }

    @Test
    @DisplayName("1. RLS em companies: Tenant Beta não enxerga nem acessa empresas cadastradas no Tenant Alpha")
    void shouldIsolateCompaniesAcrossTenants() {
        UUID alphaCompanyId = UUID.randomUUID();

        // 1. Cadastrar empresa no Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            CompanyRecord compAlpha = new CompanyRecord(
                    alphaCompanyId,
                    tenantAlphaId,
                    "Alpha Comércio e Logística LTDA",
                    "Alpha Log",
                    "33000167000101",
                    "123456",
                    "SIMPLES_NACIONAL",
                    true,
                    "Empresa do tenant Alpha",
                    Instant.now(),
                    Instant.now()
            );
            companyRepository.save(compAlpha);
            return null;
        });

        // 2. Verificar que Tenant Alpha enxerga a empresa
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            Optional<CompanyRecord> found = companyRepository.findById(alphaCompanyId);
            assertThat(found).isPresent();
            assertThat(found.get().getCorporateName()).isEqualTo("Alpha Comércio e Logística LTDA");

            List<CompanyRecord> all = companyRepository.findAllByTenantIdOrderByCorporateNameAsc(tenantAlphaId);
            assertThat(all).hasSize(1);
            return null;
        });

        // 3. Conectar como Tenant Beta e garantir que NADA do Tenant Alpha é retornado via RLS
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            Optional<CompanyRecord> notFound = companyRepository.findById(alphaCompanyId);
            assertThat(notFound).isEmpty();

            List<CompanyRecord> betaList = companyRepository.findAllByTenantIdOrderByCorporateNameAsc(tenantBetaId);
            assertThat(betaList).isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("2. RLS em company_seller_account_links: Tenant Beta não acessa vínculos de contas do Tenant Alpha")
    void shouldIsolateCompanyAccountLinksAcrossTenants() {
        UUID alphaCompanyId = UUID.randomUUID();
        UUID alphaAccountId = UUID.randomUUID();
        UUID alphaLinkId = UUID.randomUUID();

        // 1. Criar empresa, conta e vínculo no Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            CompanyRecord compAlpha = companyRepository.save(new CompanyRecord(
                    alphaCompanyId, tenantAlphaId, "Alpha Store LTDA", null, "00000000000191",
                    null, null, true, null, Instant.now(), Instant.now()
            ));

            SellerAccount accAlpha = sellerAccountRepository.save(new SellerAccount(
                    alphaAccountId, tenantAlphaId, "MERCADO_LIVRE", "MLB_ALPHA_10", "Loja Alpha Meli", "ACTIVE"
            ));

            CompanyAccountLinkRecord link = linkRepository.save(new CompanyAccountLinkRecord(
                    alphaLinkId, tenantAlphaId, compAlpha.getId(), accAlpha.getId(),
                    Instant.now(), null, true, 1, "Vínculo Alpha", "admin@alpha.com", Instant.now(), Instant.now()
            ));

            assertThat(link).isNotNull();
            return null;
        });

        // 2. Conectar como Tenant Beta e verificar isolamento total
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            Optional<CompanyAccountLinkRecord> linkBeta = linkRepository.findById(alphaLinkId);
            assertThat(linkBeta).isEmpty();

            List<CompanyAccountLinkRecord> activeLinksBeta = linkRepository.findAllActiveByCompany(tenantBetaId, alphaCompanyId);
            assertThat(activeLinksBeta).isEmpty();

            List<CompanyAccountLinkRecord> historyBeta = linkRepository.findHistoryBySellerAccount(tenantBetaId, alphaAccountId);
            assertThat(historyBeta).isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("3. RLS Violação direta: Inserção com tenant_id cruzado dispara exceção de segurança no PostgreSQL")
    void shouldThrowSecurityExceptionOnCrossTenantInsertion() {
        assertThatThrownBy(() -> transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            CompanyRecord forgedCompany = new CompanyRecord(
                    UUID.randomUUID(),
                    tenantAlphaId, // Tentativa de forjar tenant Alpha na sessão Beta
                    "Empresa Forjada LTDA",
                    null,
                    "60701190000104",
                    null,
                    null,
                    true,
                    null,
                    Instant.now(),
                    Instant.now()
            );

            companyRepository.saveAndFlush(forgedCompany);
            return null;
        })).isInstanceOf(DataAccessException.class);
    }
}
