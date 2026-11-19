package com.sellerintelligence.modules.financialanalytics;

import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.core.tenant.TenantDatabaseSession;
import com.sellerintelligence.modules.catalog.SellerAccount;
import com.sellerintelligence.modules.catalog.SellerAccountRepository;
import com.sellerintelligence.modules.financialanalytics.entity.OrderFinancialFactRecord;
import com.sellerintelligence.modules.financialanalytics.entity.OrderItemFinancialFactRecord;
import com.sellerintelligence.modules.financialanalytics.model.FinancialValueProvenance;
import com.sellerintelligence.modules.financialanalytics.model.ReconciliationStatus;
import com.sellerintelligence.modules.financialanalytics.repository.OrderFinancialFactRepository;
import com.sellerintelligence.modules.financialanalytics.repository.OrderItemFinancialFactRepository;
import com.sellerintelligence.modules.orderingestion.Order;
import com.sellerintelligence.modules.orderingestion.OrderItem;
import com.sellerintelligence.modules.orderingestion.OrderItemRepository;
import com.sellerintelligence.modules.orderingestion.OrderRepository;
import com.sellerintelligence.modules.orderingestion.model.OrderStatus;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Teste de integração PostgreSQL real via Testcontainers validando Row-Level Security (RLS)
 * em order_financial_facts e order_item_financial_facts (P017-BE).
 */
@Testcontainers
@SpringBootTest
class PostgresFinancialFactsRlsIntegrationTest {

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
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderFinancialFactRepository orderFinancialFactRepository;

    @Autowired
    private OrderItemFinancialFactRepository orderItemFinancialFactRepository;

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
                    "Alpha Financial Org",
                    "alpha-fin-" + tenantAlphaId.toString().substring(0, 8),
                    "ACTIVE",
                    Instant.now(),
                    Instant.now()
            );
            Organization orgBeta = new Organization(
                    tenantBetaId,
                    "Beta Financial Org",
                    "beta-fin-" + tenantBetaId.toString().substring(0, 8),
                    "ACTIVE",
                    Instant.now(),
                    Instant.now()
            );
            organizationRepository.saveAll(List.of(orgAlpha, orgBeta));
            return null;
        });
    }

    @Test
    @DisplayName("1. RLS em order_financial_facts: Tenant Beta não enxerga fatos financeiros de pedidos do Tenant Alpha")
    void shouldIsolateOrderFinancialFactsAcrossTenants() {
        UUID alphaOrderId = UUID.randomUUID();
        UUID alphaOrderFactId = UUID.randomUUID();

        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            SellerAccount acc = sellerAccountRepository.save(new SellerAccount(
                    UUID.randomUUID(), tenantAlphaId, "MERCADO_LIVRE", "MLB-RLS-01", "Loja Alpha", "ACTIVE"
            ));

            Order ord = orderRepository.save(new Order(
                    alphaOrderId, tenantAlphaId, acc.getId(), "MERCADO_LIVRE", "MLB-ORDER-RLS-1",
                    OrderStatus.PAID, new BigDecimal("100.0000"), BigDecimal.ZERO, new BigDecimal("100.0000"),
                    "BRL", "Cliente Alpha", "123", "TRK", null, null, null, "{}", "v1", "API",
                    Instant.now(), Instant.now()
            ));

            OrderFinancialFactRecord fact = new OrderFinancialFactRecord(
                    alphaOrderFactId, tenantAlphaId, ord.getId(), acc.getId(), null, "MERCADO_LIVRE",
                    "MLB-ORDER-RLS-1", "PAID", Instant.now(), new BigDecimal("100.0000"), BigDecimal.ZERO,
                    new BigDecimal("100.0000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("14.0000"),
                    new BigDecimal("6.0000"), BigDecimal.ZERO, new BigDecimal("40.0000"), BigDecimal.ZERO,
                    BigDecimal.ZERO, new BigDecimal("6.0000"), BigDecimal.ZERO, new BigDecimal("66.0000"),
                    new BigDecimal("34.0000"), new BigDecimal("34.0000"), "BRL", ReconciliationStatus.ESTIMATED,
                    FinancialValueProvenance.ESTIMATED, FinancialValueProvenance.ESTIMATED,
                    FinancialValueProvenance.USER_INFORMED, FinancialValueProvenance.USER_INFORMED, false, null,
                    "v1.0.0", "{}"
            );
            orderFinancialFactRepository.save(fact);
            return null;
        });

        // 2. Verificar que Tenant Alpha enxerga o fato
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            Optional<OrderFinancialFactRecord> found = orderFinancialFactRepository.findById(alphaOrderFactId);
            assertThat(found).isPresent();
            assertThat(found.get().getExternalOrderId()).isEqualTo("MLB-ORDER-RLS-1");
            return null;
        });

        // 3. Garantir que Tenant Beta não enxerga NADA via RLS
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            Optional<OrderFinancialFactRecord> notFound = orderFinancialFactRepository.findById(alphaOrderFactId);
            assertThat(notFound).isEmpty();

            Optional<OrderFinancialFactRecord> notFoundByOrder =
                    orderFinancialFactRepository.findByTenantIdAndOrderId(tenantBetaId, alphaOrderId);
            assertThat(notFoundByOrder).isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("2. RLS em order_item_financial_facts: Tenant Beta não acessa itens de fatos financeiros do Tenant Alpha")
    void shouldIsolateOrderItemFinancialFactsAcrossTenants() {
        UUID alphaOrderId = UUID.randomUUID();
        UUID alphaOrderFactId = UUID.randomUUID();
        UUID alphaItemFactId = UUID.randomUUID();
        UUID alphaOrderItemId = UUID.randomUUID();

        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            SellerAccount acc = sellerAccountRepository.save(new SellerAccount(
                    UUID.randomUUID(), tenantAlphaId, "MERCADO_LIVRE", "MLB-RLS-02", "Loja Alpha 2", "ACTIVE"
            ));

            Order ord = orderRepository.save(new Order(
                    alphaOrderId, tenantAlphaId, acc.getId(), "MERCADO_LIVRE", "MLB-ORDER-RLS-2",
                    OrderStatus.PAID, new BigDecimal("150.0000"), BigDecimal.ZERO, new BigDecimal("150.0000"),
                    "BRL", "Cliente Alpha", "123", "TRK", null, null, null, "{}", "v1", "API",
                    Instant.now(), Instant.now()
            ));

            OrderItem ordItem = orderItemRepository.save(new OrderItem(
                    alphaOrderItemId, tenantAlphaId, ord.getId(), null, null, "MLB-L", "MLB-I",
                    "SKU-RLS-01", "Item RLS", new BigDecimal("150.0000"), 1, new BigDecimal("150.0000")
            ));

            OrderFinancialFactRecord fact = orderFinancialFactRepository.save(new OrderFinancialFactRecord(
                    alphaOrderFactId, tenantAlphaId, ord.getId(), acc.getId(), null, "MERCADO_LIVRE",
                    "MLB-ORDER-RLS-2", "PAID", Instant.now(), new BigDecimal("150.0000"), BigDecimal.ZERO,
                    new BigDecimal("150.0000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("21.0000"),
                    new BigDecimal("6.0000"), BigDecimal.ZERO, new BigDecimal("50.0000"), BigDecimal.ZERO,
                    BigDecimal.ZERO, new BigDecimal("9.0000"), BigDecimal.ZERO, new BigDecimal("86.0000"),
                    new BigDecimal("64.0000"), new BigDecimal("42.6667"), "BRL", ReconciliationStatus.ESTIMATED,
                    FinancialValueProvenance.ESTIMATED, FinancialValueProvenance.ESTIMATED,
                    FinancialValueProvenance.USER_INFORMED, FinancialValueProvenance.USER_INFORMED, false, null,
                    "v1.0.0", "{}"
            ));

            OrderItemFinancialFactRecord itemFact = orderItemFinancialFactRepository.save(new OrderItemFinancialFactRecord(
                    alphaItemFactId, tenantAlphaId, fact.getId(), ord.getId(), ordItem.getId(), null, null,
                    "SKU-RLS-01", "Item RLS", 1, new BigDecimal("150.0000"), new BigDecimal("150.0000"),
                    BigDecimal.ZERO, new BigDecimal("150.0000"), BigDecimal.ZERO, BigDecimal.ZERO,
                    new BigDecimal("21.0000"), new BigDecimal("6.0000"), BigDecimal.ZERO, new BigDecimal("50.0000"),
                    new BigDecimal("50.0000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("6.0000"),
                    new BigDecimal("9.0000"), BigDecimal.ZERO, new BigDecimal("86.0000"), new BigDecimal("64.0000"),
                    new BigDecimal("42.6667"), FinancialValueProvenance.USER_INFORMED, false, null
            ));
            return null;
        });

        // 2. Tenant Alpha enxerga
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            Optional<OrderItemFinancialFactRecord> found = orderItemFinancialFactRepository.findById(alphaItemFactId);
            assertThat(found).isPresent();
            assertThat(found.get().getSku()).isEqualTo("SKU-RLS-01");

            List<OrderItemFinancialFactRecord> items =
                    orderItemFinancialFactRepository.findAllByTenantIdAndOrderFactIdOrderByCreatedAtAsc(tenantAlphaId, alphaOrderFactId);
            assertThat(items).hasSize(1);
            return null;
        });

        // 3. Tenant Beta não enxerga
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            Optional<OrderItemFinancialFactRecord> notFound = orderItemFinancialFactRepository.findById(alphaItemFactId);
            assertThat(notFound).isEmpty();

            List<OrderItemFinancialFactRecord> betaItems =
                    orderItemFinancialFactRepository.findAllByTenantIdAndOrderFactIdOrderByCreatedAtAsc(tenantBetaId, alphaOrderFactId);
            assertThat(betaItems).isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("3. RLS Check Violation: Tentativa de inserção com tenant adulterado é bloqueada pelo PostgreSQL")
    void shouldPreventCrossTenantDataTamperingViaRlsPolicy() {
        assertThatThrownBy(() -> transactionTemplate.execute(status -> {
            // Sessão configurada para Tenant Beta
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            // Tentativa de criar fato com tenant_id do Tenant Alpha
            OrderFinancialFactRecord maliciousFact = new OrderFinancialFactRecord(
                    UUID.randomUUID(),
                    tenantAlphaId, // adulteração cross-tenant!
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    null,
                    "MERCADO_LIVRE",
                    "MALICIOUS-001",
                    "PAID",
                    Instant.now(),
                    new BigDecimal("100.0000"),
                    BigDecimal.ZERO,
                    new BigDecimal("100.0000"),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    new BigDecimal("100.0000"),
                    new BigDecimal("100.0000"),
                    "BRL",
                    ReconciliationStatus.ESTIMATED,
                    FinancialValueProvenance.ESTIMATED,
                    FinancialValueProvenance.ESTIMATED,
                    FinancialValueProvenance.USER_INFORMED,
                    FinancialValueProvenance.USER_INFORMED,
                    false,
                    null,
                    "v1.0.0",
                    "{}"
            );

            orderFinancialFactRepository.saveAndFlush(maliciousFact);
            return null;
        })).isInstanceOf(DataAccessException.class);
    }
}
