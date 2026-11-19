package com.sellerintelligence.modules.pricing;

import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.core.tenant.TenantDatabaseSession;
import com.sellerintelligence.modules.pricing.entity.FeeRuleRecord;
import com.sellerintelligence.modules.pricing.entity.ProductCostRecord;
import com.sellerintelligence.modules.pricing.model.FeeSourceType;
import com.sellerintelligence.modules.pricing.model.FeeType;
import com.sellerintelligence.modules.pricing.model.FeeValueType;
import com.sellerintelligence.modules.pricing.repository.FeeRuleRepository;
import com.sellerintelligence.modules.pricing.repository.ProductCostRepository;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Teste de integração PostgreSQL real via Testcontainers para validação mandatória de RLS
 * (Row-Level Security) nas tabelas product_costs e marketplace_fee_rules (P014-BE).
 */
@Testcontainers
@SpringBootTest
class PostgresPricingRlsIntegrationTest {

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
    private ProductCostRepository costRepository;

    @Autowired
    private FeeRuleRepository feeRuleRepository;

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
                    "Alpha Pricing Org",
                    "alpha-price-" + tenantAlphaId.toString().substring(0, 8),
                    "ACTIVE",
                    Instant.now(),
                    Instant.now()
            );
            Organization orgBeta = new Organization(
                    tenantBetaId,
                    "Beta Pricing Org",
                    "beta-price-" + tenantBetaId.toString().substring(0, 8),
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
    @DisplayName("1. RLS: Tentativa de leitura cross-tenant em product_costs é bloqueada pelo PostgreSQL")
    void rlsPreventsCrossTenantProductCostReading() {
        UUID productId = UUID.randomUUID();

        // 1. Grava custo no Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            ProductCostRecord costAlpha = new ProductCostRecord(
                    UUID.randomUUID(),
                    tenantAlphaId,
                    productId,
                    null,
                    null,
                    "BRL",
                    "UN",
                    new BigDecimal("50.0000"),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    Instant.now().minusSeconds(3600),
                    null,
                    1,
                    true,
                    "alpha@test.com",
                    "Custo confidencial Alpha",
                    Instant.now(),
                    Instant.now()
            );
            costRepository.saveAndFlush(costAlpha);
            return null;
        });

        // 2. Consulta pelo Tenant Alpha -> DEVE visualizar
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);
            List<ProductCostRecord> alphaCosts = costRepository.findByTenantIdAndProductIdOrderByEffectiveFromDesc(tenantAlphaId, productId);
            assertThat(alphaCosts).hasSize(1);
            assertThat(alphaCosts.get(0).getAcquisitionCost()).isEqualByComparingTo("50.0000");
            return null;
        });

        // 3. Consulta pelo Tenant Beta -> NÃO DEVE visualizar (RLS filtra no banco)
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);
            List<ProductCostRecord> betaCosts = costRepository.findByTenantIdAndProductIdOrderByEffectiveFromDesc(tenantAlphaId, productId);
            assertThat(betaCosts).isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("2. RLS: Tentativa de inserção com spoofing de tenant em product_costs é abortada pelo PostgreSQL")
    void rlsPreventsCrossTenantProductCostInsertion() {
        UUID productId = UUID.randomUUID();

        assertThatThrownBy(() -> transactionTemplate.execute(status -> {
            // Sessão do banco configurada para Tenant Beta
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            // Objeto com tenantAlphaId
            ProductCostRecord spoofedCost = new ProductCostRecord(
                    UUID.randomUUID(),
                    tenantAlphaId, // Spoofing!
                    productId,
                    null,
                    null,
                    "BRL",
                    "UN",
                    new BigDecimal("99.0000"),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    Instant.now(),
                    null,
                    1,
                    true,
                    "attacker@test.com",
                    "Tentativa de injeção cross-tenant",
                    Instant.now(),
                    Instant.now()
            );

            costRepository.saveAndFlush(spoofedCost);
            return null;
        })).isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("3. RLS: Tentativa de leitura cross-tenant em marketplace_fee_rules é bloqueada pelo PostgreSQL")
    void rlsPreventsCrossTenantFeeRuleReading() {
        // 1. Grava regra no Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            FeeRuleRecord ruleAlpha = new FeeRuleRecord(
                    UUID.randomUUID(),
                    tenantAlphaId,
                    "MERCADO_LIVRE",
                    "CLASSIC",
                    null,
                    FeeType.COMMISSION,
                    FeeValueType.PERCENTAGE,
                    new BigDecimal("13.0000"),
                    null,
                    "BRL",
                    null,
                    null,
                    FeeSourceType.OFFICIAL_CONTRACT,
                    "Contrato Alpha Privado",
                    null,
                    Instant.now().minusSeconds(3600),
                    null,
                    1,
                    true,
                    "alpha@test.com",
                    Instant.now(),
                    Instant.now()
            );
            feeRuleRepository.saveAndFlush(ruleAlpha);
            return null;
        });

        // 2. Consulta pelo Tenant Alpha -> DEVE visualizar
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);
            List<FeeRuleRecord> alphaRules = feeRuleRepository.findByTenantIdAndMarketplaceOrderByEffectiveFromDesc(tenantAlphaId, "MERCADO_LIVRE");
            assertThat(alphaRules).hasSize(1);
            assertThat(alphaRules.get(0).getPercentageRate()).isEqualByComparingTo("13.0000");
            return null;
        });

        // 3. Consulta pelo Tenant Beta -> NÃO DEVE visualizar (RLS filtra no banco)
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);
            List<FeeRuleRecord> betaRules = feeRuleRepository.findByTenantIdAndMarketplaceOrderByEffectiveFromDesc(tenantAlphaId, "MERCADO_LIVRE");
            assertThat(betaRules).isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("4. RLS: Tentativa de inserção com spoofing de tenant em marketplace_fee_rules é abortada pelo PostgreSQL")
    void rlsPreventsCrossTenantFeeRuleInsertion() {
        assertThatThrownBy(() -> transactionTemplate.execute(status -> {
            // Sessão do banco configurada para Tenant Beta
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            // Objeto com tenantAlphaId
            FeeRuleRecord spoofedRule = new FeeRuleRecord(
                    UUID.randomUUID(),
                    tenantAlphaId, // Spoofing!
                    "SHOPEE",
                    "STANDARD",
                    null,
                    FeeType.COMMISSION,
                    FeeValueType.PERCENTAGE,
                    new BigDecimal("14.0000"),
                    null,
                    "BRL",
                    null,
                    null,
                    FeeSourceType.USER_CONFIGURED,
                    "Regra Injetada",
                    null,
                    Instant.now(),
                    null,
                    1,
                    true,
                    "attacker@test.com",
                    Instant.now(),
                    Instant.now()
            );

            feeRuleRepository.saveAndFlush(spoofedRule);
            return null;
        })).isInstanceOf(DataAccessException.class);
    }
}
