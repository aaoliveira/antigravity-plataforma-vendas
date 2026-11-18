package com.sellerintelligence.modules.orderingestion;

import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.core.tenant.TenantDatabaseSession;
import com.sellerintelligence.modules.catalog.SellerAccount;
import com.sellerintelligence.modules.catalog.SellerAccountRepository;
import com.sellerintelligence.modules.orderingestion.model.OrderStatus;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Teste de integração PostgreSQL real via Testcontainers para validação mandatória de RLS
 * (Row-Level Security) nas tabelas de Pedidos, Itens, Checkpoints, Outbox e DLQ (P008-BE).
 */
@Testcontainers
@SpringBootTest
class PostgresOrdersRlsIntegrationTest {

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
    private OrderSyncCheckpointRepository checkpointRepository;

    @Autowired
    private OrderOutboxEventRepository outboxEventRepository;

    @Autowired
    private OrderDlqRecordRepository dlqRecordRepository;

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
                    "Alpha Orders Org",
                    "alpha-ord-" + tenantAlphaId.toString().substring(0, 8),
                    "ACTIVE",
                    Instant.now(),
                    Instant.now()
            );
            Organization orgBeta = new Organization(
                    tenantBetaId,
                    "Beta Orders Org",
                    "beta-ord-" + tenantBetaId.toString().substring(0, 8),
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
    @DisplayName("RLS: Tentativa de leitura cross-tenant em pedidos e itens é bloqueada pelo PostgreSQL")
    void rlsPreventsCrossTenantOrdersAndItemsReading() {
        UUID orderAlphaId = UUID.randomUUID();
        UUID itemAlphaId = UUID.randomUUID();

        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            SellerAccount accountAlpha = sellerAccountRepository.save(new SellerAccount(
                    UUID.randomUUID(),
                    tenantAlphaId,
                    "MERCADO_LIVRE",
                    "SELLER-ALPHA-ORD",
                    "Loja Alpha Pedidos",
                    "ACTIVE"
            ));

            Order orderAlpha = new Order(
                    orderAlphaId,
                    tenantAlphaId,
                    accountAlpha.getId(),
                    "MERCADO_LIVRE",
                    "ORD-ALPHA-EXT-01",
                    OrderStatus.PAID,
                    new BigDecimal("350.00"),
                    new BigDecimal("25.00"),
                    new BigDecimal("375.00"),
                    "BRL",
                    "Comprador Alpha",
                    "***.111.222-**",
                    "TRK-ALPHA-01",
                    null,
                    null,
                    null,
                    "{}",
                    "v1",
                    "MERCADO_LIVRE",
                    Instant.now(),
                    Instant.now()
            );
            orderRepository.save(orderAlpha);

            OrderItem itemAlpha = new OrderItem(
                    itemAlphaId,
                    tenantAlphaId,
                    orderAlphaId,
                    null,
                    null,
                    "MLB-ALPHA-99",
                    "ITM-EXT-1",
                    "SKU-ALPHA-1",
                    "Item Alpha RLS",
                    new BigDecimal("350.00"),
                    1,
                    new BigDecimal("350.00")
            );
            orderItemRepository.save(itemAlpha);

            return null;
        });

        // Executa sessão com Tenant Beta: RLS deve ocultar completamente os pedidos e itens do Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            List<UUID> visibleOrders = jdbcTemplate.queryForList(
                    "SELECT id FROM orders",
                    UUID.class
            );
            assertThat(visibleOrders).isEmpty();

            List<UUID> visibleItems = jdbcTemplate.queryForList(
                    "SELECT id FROM order_items",
                    UUID.class
            );
            assertThat(visibleItems).isEmpty();

            return null;
        });
    }

    @Test
    @DisplayName("RLS: Tentativa de escrita com spoofing de tenant em pedidos é rejeitada com violação de RLS")
    void rlsPreventsCrossTenantOrderInsertion() {
        UUID accountAlphaId = UUID.randomUUID();

        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);
            sellerAccountRepository.save(new SellerAccount(
                    accountAlphaId,
                    tenantAlphaId,
                    "MERCADO_LIVRE",
                    "SELLER-ACC-SPOOF",
                    "Loja Spoof",
                    "ACTIVE"
            ));
            return null;
        });

        transactionTemplate.execute(status -> {
            // Sessão pertence ao Tenant Alpha
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            // Tentativa deliberada de inserir pedido associado ao Tenant Beta
            assertThatThrownBy(() -> jdbcTemplate.update(
                    "INSERT INTO orders (id, tenant_id, seller_account_id, marketplace, external_order_id, status, subtotal, shipping_cost, total_amount, currency, origin, schema_version, order_created_at, synced_at, created_at, updated_at) " +
                    "VALUES (?, ?, ?, 'MERCADO_LIVRE', 'ORD-SPOOF-999', 'PAID', 100.0, 0.0, 100.0, 'BRL', 'MERCADO_LIVRE', 'v1', NOW(), NOW(), NOW(), NOW())",
                    UUID.randomUUID(),
                    tenantBetaId, // Spoofing: tenant_id diverge da sessão app.current_tenant
                    accountAlphaId
            )).isInstanceOf(DataAccessException.class);

            return null;
        });
    }

    @Test
    @DisplayName("RLS: Checkpoints, Outbox Events e DLQ são estritamente isolados por RLS")
    void rlsIsolatesCheckpointsOutboxAndDlq() {
        UUID accountAlphaId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            sellerAccountRepository.save(new SellerAccount(
                    accountAlphaId,
                    tenantAlphaId,
                    "SHOPEE",
                    "SHOP-ALPHA-ORD",
                    "Shopee Alpha",
                    "ACTIVE"
            ));

            checkpointRepository.save(new OrderSyncCheckpoint(
                    UUID.randomUUID(),
                    tenantAlphaId,
                    accountAlphaId,
                    "SHOPEE",
                    "COMPLETED",
                    "cursor-alpha",
                    5,
                    50,
                    5
            ));

            outboxEventRepository.save(new OrderOutboxEvent(
                    UUID.randomUUID(),
                    tenantAlphaId,
                    "ORDER_SYNCED",
                    "ORDER",
                    orderId,
                    "{\"orderId\":\"" + orderId + "\"}"
            ));

            dlqRecordRepository.save(new OrderDlqRecord(
                    UUID.randomUUID(),
                    tenantAlphaId,
                    accountAlphaId,
                    "SHOPEE",
                    "ORD-CORRUPTED-1",
                    "{}",
                    "Erro isolado"
            ));

            return null;
        });

        // Sessão do Tenant Beta não deve enxergar nada do Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            assertThat(jdbcTemplate.queryForList("SELECT id FROM order_sync_checkpoints", UUID.class)).isEmpty();
            assertThat(jdbcTemplate.queryForList("SELECT id FROM order_outbox_events", UUID.class)).isEmpty();
            assertThat(jdbcTemplate.queryForList("SELECT id FROM order_dlq_records", UUID.class)).isEmpty();

            return null;
        });
    }
}
