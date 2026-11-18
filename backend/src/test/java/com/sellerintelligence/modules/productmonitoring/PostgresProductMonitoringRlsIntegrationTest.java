package com.sellerintelligence.modules.productmonitoring;

import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.core.tenant.TenantDatabaseSession;
import com.sellerintelligence.modules.productmonitoring.entity.MonitoredItemRecord;
import com.sellerintelligence.modules.productmonitoring.entity.MonitoredItemSnapshotRecord;
import com.sellerintelligence.modules.productmonitoring.entity.MonitoringGroupRecord;
import com.sellerintelligence.modules.productmonitoring.model.MonitoredItemStatus;
import com.sellerintelligence.modules.productmonitoring.model.MonitoringGroupStatus;
import com.sellerintelligence.modules.productmonitoring.repository.MonitoredItemRepository;
import com.sellerintelligence.modules.productmonitoring.repository.MonitoredItemSnapshotRepository;
import com.sellerintelligence.modules.productmonitoring.repository.MonitoringGroupRepository;
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
 * Teste de integração PostgreSQL real via Testcontainers para validação mandatória de RLS
 * (Row-Level Security) nas tabelas de Grupos de Monitoramento, Itens e Snapshots (P012-BE).
 */
@Testcontainers
@SpringBootTest
class PostgresProductMonitoringRlsIntegrationTest {

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
    private MonitoringGroupRepository groupRepository;

    @Autowired
    private MonitoredItemRepository itemRepository;

    @Autowired
    private MonitoredItemSnapshotRepository snapshotRepository;

    @Autowired
    private TenantDatabaseSession tenantDatabaseSession;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

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
                    "Alpha Monitoring Org",
                    "alpha-mon-" + tenantAlphaId.toString().substring(0, 8),
                    "ACTIVE",
                    Instant.now(),
                    Instant.now()
            );
            Organization orgBeta = new Organization(
                    tenantBetaId,
                    "Beta Monitoring Org",
                    "beta-mon-" + tenantBetaId.toString().substring(0, 8),
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
    @DisplayName("RLS: Tentativa de leitura cross-tenant em monitoring_groups é bloqueada pelo PostgreSQL")
    void rlsPreventsCrossTenantMonitoringGroupReading() {
        UUID groupAlphaId = UUID.randomUUID();

        // 1. Grava grupo no Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            MonitoringGroupRecord groupAlpha = new MonitoringGroupRecord(
                    groupAlphaId,
                    tenantAlphaId,
                    "Grupo Confidencial Alpha",
                    "Apenas Alpha pode visualizar",
                    "#3B82F6",
                    60,
                    90,
                    MonitoringGroupStatus.ACTIVE,
                    null,
                    Instant.now(),
                    Instant.now()
            );
            groupRepository.saveAndFlush(groupAlpha);
            return null;
        });

        // 2. Consulta pelo Tenant Alpha (deve encontrar)
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);
            Optional<MonitoringGroupRecord> found = groupRepository.findById(groupAlphaId);
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Grupo Confidencial Alpha");
            return null;
        });

        // 3. Consulta pelo Tenant Beta (RLS deve bloquear e retornar vazio)
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);
            Optional<MonitoringGroupRecord> found = groupRepository.findById(groupAlphaId);
            assertThat(found).isEmpty();

            List<MonitoringGroupRecord> betaList = groupRepository.findAll();
            assertThat(betaList).noneMatch(g -> g.getId().equals(groupAlphaId));
            return null;
        });
    }

    @Test
    @DisplayName("RLS: Tentativa de leitura cross-tenant em monitored_items e snapshots é bloqueada pelo PostgreSQL")
    void rlsPreventsCrossTenantItemAndSnapshotReading() {
        UUID groupAlphaId = UUID.randomUUID();
        UUID itemAlphaId = UUID.randomUUID();
        UUID snapshotAlphaId = UUID.randomUUID();

        // 1. Grava no Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            MonitoringGroupRecord group = new MonitoringGroupRecord(
                    groupAlphaId, tenantAlphaId, "Alpha Eletrônicos", null, "#3B82F6",
                    60, 90, MonitoringGroupStatus.ACTIVE, null, Instant.now(), Instant.now()
            );
            groupRepository.saveAndFlush(group);

            MonitoredItemRecord item = new MonitoredItemRecord(
                    itemAlphaId, tenantAlphaId, groupAlphaId, "MERCADO_LIVRE", "POL-1", "MLB-CONFIDENCIAL",
                    "iPhone 15 Pro Max Alpha", new BigDecimal("7999.00"), new BigDecimal("7999.00"), "BRL",
                    null, null, "NEW", MonitoredItemStatus.ACTIVE, 60, null, Instant.now(),
                    null, null, Instant.now(), Instant.now()
            );
            itemRepository.saveAndFlush(item);

            MonitoredItemSnapshotRecord snapshot = new MonitoredItemSnapshotRecord(
                    snapshotAlphaId, tenantAlphaId, itemAlphaId, groupAlphaId, "MERCADO_LIVRE", "MLB-CONFIDENCIAL",
                    new BigDecimal("7999.00"), "BRL", BigDecimal.ZERO, "FREE", 20, "ACTIVE",
                    "OBSERVED", Instant.now(), Instant.now(), true, Instant.now()
            );
            snapshotRepository.saveAndFlush(snapshot);
            return null;
        });

        // 2. Consulta pelo Tenant Beta (RLS deve retornar vazio para item e snapshot)
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            Optional<MonitoredItemRecord> itemBeta = itemRepository.findById(itemAlphaId);
            assertThat(itemBeta).isEmpty();

            Optional<MonitoredItemSnapshotRecord> snapshotBeta = snapshotRepository.findById(snapshotAlphaId);
            assertThat(snapshotBeta).isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("RLS: Tentativa de inserção forjando tenant_id de outro tenant é bloqueada pelo WITH CHECK")
    void rlsPreventsCrossTenantSpoofingInsert() {
        assertThatThrownBy(() -> {
            transactionTemplate.execute(status -> {
                tenantDatabaseSession.setTenantSession(tenantAlphaId);

                // Tenta inserir grupo com tenant_id do Tenant Beta enquanto sessão é Alpha
                MonitoringGroupRecord forgedGroup = new MonitoringGroupRecord(
                        UUID.randomUUID(),
                        tenantBetaId, // FORJADO
                        "Grupo Invasor",
                        "Tentativa de spoofing",
                        "#EF4444",
                        60,
                        90,
                        MonitoringGroupStatus.ACTIVE,
                        null,
                        Instant.now(),
                        Instant.now()
                );

                groupRepository.saveAndFlush(forgedGroup);
                return null;
            });
        }).isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("RLS: Tentativa de mutação (UPDATE/DELETE) cross-tenant afeta 0 linhas")
    void rlsPreventsCrossTenantMutations() {
        UUID groupAlphaId = UUID.randomUUID();

        // 1. Cria grupo como Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            MonitoringGroupRecord group = new MonitoringGroupRecord(
                    groupAlphaId, tenantAlphaId, "Grupo Protegido", null, "#3B82F6",
                    60, 90, MonitoringGroupStatus.ACTIVE, null, Instant.now(), Instant.now()
            );
            groupRepository.saveAndFlush(group);
            return null;
        });

        // 2. Tenant Beta tenta deletar o grupo de Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);
            groupRepository.deleteById(groupAlphaId);
            groupRepository.flush();
            return null;
        });

        // 3. Verifica que o grupo de Alpha permanece intacto
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);
            Optional<MonitoringGroupRecord> found = groupRepository.findById(groupAlphaId);
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Grupo Protegido");
            return null;
        });
    }

    @Test
    @DisplayName("RLS: Sessão sem tenant ativo vê 0 registros em tabelas protegidas")
    void rlsReturnsZeroRowsWhenNoTenantSessionIsSet() {
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            MonitoringGroupRecord group = new MonitoringGroupRecord(
                    UUID.randomUUID(), tenantAlphaId, "Grupo Alpha", null, "#3B82F6",
                    60, 90, MonitoringGroupStatus.ACTIVE, null, Instant.now(), Instant.now()
            );
            groupRepository.saveAndFlush(group);
            return null;
        });

        transactionTemplate.execute(status -> {
            jdbcTemplate.execute("RESET app.current_tenant;");

            List<MonitoringGroupRecord> groups = groupRepository.findAll();
            assertThat(groups).isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("P013-BE RLS: Unicidade de Idempotency Key é isolada por Tenant no PostgreSQL 17")
    void rlsValidatesTenantScopedIdempotencyKeyConstraint() {
        UUID groupAlphaId = UUID.randomUUID();
        UUID itemAlphaId = UUID.randomUUID();
        UUID groupBetaId = UUID.randomUUID();
        UUID itemBetaId = UUID.randomUUID();
        String sharedKey = "idem-key-postgres-17";

        // 1. Cria grupo e item no Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);
            MonitoringGroupRecord g = new MonitoringGroupRecord(
                    groupAlphaId, tenantAlphaId, "Grupo Alpha Idem", null, "#3B82F6",
                    60, 90, MonitoringGroupStatus.ACTIVE, null, Instant.now(), Instant.now()
            );
            groupRepository.saveAndFlush(g);

            MonitoredItemRecord i = new MonitoredItemRecord(
                    itemAlphaId, tenantAlphaId, groupAlphaId, "MERCADO_LIVRE", "POL-1", "MLB-A1",
                    "Item Alpha", new BigDecimal("100.00"), new BigDecimal("100.00"), "BRL",
                    null, null, "NEW", MonitoredItemStatus.ACTIVE, 60, null, Instant.now(),
                    null, null, Instant.now(), Instant.now()
            );
            itemRepository.saveAndFlush(i);

            MonitoredItemSnapshotRecord s = new MonitoredItemSnapshotRecord(
                    UUID.randomUUID(), tenantAlphaId, itemAlphaId, groupAlphaId, "MERCADO_LIVRE", "MLB-A1",
                    new BigDecimal("100.00"), "BRL", BigDecimal.ZERO, "FREE", 10, "ACTIVE",
                    "OBSERVED", Instant.now(), Instant.now(), Instant.now().plusSeconds(3600), true,
                    sharedKey, null, "OFFICIAL_API", "SUCCESS", null, false,
                    BigDecimal.ZERO, BigDecimal.ZERO, false, false, "INITIAL", Instant.now()
            );
            snapshotRepository.saveAndFlush(s);
            return null;
        });

        // 2. Tenant Alpha tenta inserir snapshot DUPLICADO com a mesma chave -> DEVE FALHAR (Unique Constraint)
        assertThatThrownBy(() -> {
            transactionTemplate.execute(status -> {
                tenantDatabaseSession.setTenantSession(tenantAlphaId);
                MonitoredItemSnapshotRecord duplicate = new MonitoredItemSnapshotRecord(
                        UUID.randomUUID(), tenantAlphaId, itemAlphaId, groupAlphaId, "MERCADO_LIVRE", "MLB-A1",
                        new BigDecimal("100.00"), "BRL", BigDecimal.ZERO, "FREE", 10, "ACTIVE",
                        "OBSERVED", Instant.now(), Instant.now(), Instant.now().plusSeconds(3600), true,
                        sharedKey, null, "OFFICIAL_API", "SUCCESS", null, false,
                        BigDecimal.ZERO, BigDecimal.ZERO, false, false, "INITIAL", Instant.now()
                );
                snapshotRepository.saveAndFlush(duplicate);
                return null;
            });
        }).isInstanceOf(DataAccessException.class);

        // 3. Tenant Beta usa a MESMA chave "sharedKey" no seu próprio tenant -> DEVE TER SUCESSO (Chave isolada por tenant)
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);
            MonitoringGroupRecord gBeta = new MonitoringGroupRecord(
                    groupBetaId, tenantBetaId, "Grupo Beta Idem", null, "#10B981",
                    60, 90, MonitoringGroupStatus.ACTIVE, null, Instant.now(), Instant.now()
            );
            groupRepository.saveAndFlush(gBeta);

            MonitoredItemRecord iBeta = new MonitoredItemRecord(
                    itemBetaId, tenantBetaId, groupBetaId, "MERCADO_LIVRE", "POL-1", "MLB-B1",
                    "Item Beta", new BigDecimal("200.00"), new BigDecimal("200.00"), "BRL",
                    null, null, "NEW", MonitoredItemStatus.ACTIVE, 60, null, Instant.now(),
                    null, null, Instant.now(), Instant.now()
            );
            itemRepository.saveAndFlush(iBeta);

            MonitoredItemSnapshotRecord sBeta = new MonitoredItemSnapshotRecord(
                    UUID.randomUUID(), tenantBetaId, itemBetaId, groupBetaId, "MERCADO_LIVRE", "MLB-B1",
                    new BigDecimal("200.00"), "BRL", BigDecimal.ZERO, "FREE", 5, "ACTIVE",
                    "OBSERVED", Instant.now(), Instant.now(), Instant.now().plusSeconds(3600), true,
                    sharedKey, null, "OFFICIAL_API", "SUCCESS", null, false,
                    BigDecimal.ZERO, BigDecimal.ZERO, false, false, "INITIAL", Instant.now()
            );
            snapshotRepository.saveAndFlush(sBeta);

            Optional<MonitoredItemSnapshotRecord> found = snapshotRepository.findByTenantIdAndIdempotencyKey(tenantBetaId, sharedKey);
            assertThat(found).isPresent();
            assertThat(found.get().getItemId()).isEqualTo(itemBetaId);
            return null;
        });
    }
}
