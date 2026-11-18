package com.sellerintelligence.modules.catalog;

import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.core.tenant.TenantDatabaseSession;
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
 * Teste de integração PostgreSQL real via Testcontainers para validação de RLS (Row-Level Security)
 * nas entidades de Catálogo e Anúncios (P007-BE).
 */
@Testcontainers
@SpringBootTest
class PostgresCatalogRlsIntegrationTest {

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
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private MarketplaceListingRepository listingRepository;

    @Autowired
    private ExternalIdentifierMappingRepository externalMappingRepository;

    @Autowired
    private ListingSyncCheckpointRepository checkpointRepository;

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
                    "Alpha Catalog Org",
                    "alpha-cat-" + tenantAlphaId.toString().substring(0, 8),
                    "ACTIVE",
                    Instant.now(),
                    Instant.now()
            );
            Organization orgBeta = new Organization(
                    tenantBetaId,
                    "Beta Catalog Org",
                    "beta-cat-" + tenantBetaId.toString().substring(0, 8),
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
    @DisplayName("RLS: Tentativa de leitura cross-tenant em produtos e anúncios é categoricamente bloqueada pelo PostgreSQL")
    void testCrossTenantReadBlockedByRls() {
        UUID alphaProductId = UUID.randomUUID();
        UUID alphaListingId = UUID.randomUUID();
        UUID alphaAccountId = UUID.randomUUID();

        // 1. Tenant Alpha insere dados na sua sessão RLS
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            SellerAccount account = new SellerAccount(
                    alphaAccountId,
                    tenantAlphaId,
                    "MERCADO_LIVRE",
                    "ALPHA_STORE_01",
                    "Loja Alpha",
                    "ACTIVE"
            );
            sellerAccountRepository.save(account);

            Product product = new Product(
                    alphaProductId,
                    tenantAlphaId,
                    "Fone Bluetooth Pro Alpha",
                    "FONE-BT-PRO",
                    "AudioPro",
                    "Audio",
                    "ACTIVE"
            );
            productRepository.save(product);

            MarketplaceListing listing = new MarketplaceListing(
                    alphaListingId,
                    tenantAlphaId,
                    alphaAccountId,
                    product.getId(),
                    null,
                    "MERCADO_LIVRE",
                    "MLB-ALPHA-999",
                    "SKU-ALPHA-999",
                    null,
                    "Fone Bluetooth Pro Alpha 100m",
                    new BigDecimal("199.90"),
                    "BRL",
                    10,
                    "ACTIVE",
                    "https://mercadolivre.com/alpha",
                    "{\"id\":\"MLB-ALPHA-999\"}",
                    "v1",
                    "MERCADO_LIVRE_ITEMS_API"
            );
            listingRepository.save(listing);
            return null;
        });

        // 2. Tenant Beta tenta ler dados do Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            // Leitura de produto por ID
            Optional<Product> prod = productRepository.findById(alphaProductId);
            assertThat(prod).isEmpty();

            // Leitura de anúncio por ID
            Optional<MarketplaceListing> listing = listingRepository.findById(alphaListingId);
            assertThat(listing).isEmpty();

            // Leitura de conta por ID
            Optional<SellerAccount> account = sellerAccountRepository.findById(alphaAccountId);
            assertThat(account).isEmpty();

            // Raw query no PostgreSQL RLS
            List<String> rawProducts = jdbcTemplate.query(
                    "SELECT title FROM products WHERE id = ?",
                    (rs, rowNum) -> rs.getString("title"),
                    alphaProductId
            );
            assertThat(rawProducts).isEmpty();

            List<String> rawListings = jdbcTemplate.query(
                    "SELECT title FROM marketplace_listings WHERE id = ?",
                    (rs, rowNum) -> rs.getString("title"),
                    alphaListingId
            );
            assertThat(rawListings).isEmpty();

            return null;
        });
    }

    @Test
    @DisplayName("RLS: Tentativa de inserção cross-tenant viola política WITH CHECK do PostgreSQL")
    void testCrossTenantCreateBlockedByRls() {
        // Tenant Beta tenta inserir produto pertencente ao Tenant Alpha
        assertThatThrownBy(() -> {
            transactionTemplate.execute(status -> {
                tenantDatabaseSession.setTenantSession(tenantBetaId);

                Product forgedProduct = new Product(
                        UUID.randomUUID(),
                        tenantAlphaId, // Spoofing do Tenant Alpha
                        "Produto Forjado",
                        "FORGED-SKU",
                        "FakeBrand",
                        "Test",
                        "ACTIVE"
                );
                productRepository.save(forgedProduct);
                productRepository.flush();
                return null;
            });
        }).isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("RLS: Transação sem tenant configurado no PostgreSQL resulta em isolamento absoluto (zero visibilidade)")
    void testUnsetTenantSessionReturnsNoData() {
        UUID alphaProductId = UUID.randomUUID();

        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);
            Product product = new Product(
                    alphaProductId,
                    tenantAlphaId,
                    "Notebook Gamer",
                    "NOTE-GM-01",
                    "TechCorp",
                    "Computadores",
                    "ACTIVE"
            );
            productRepository.save(product);
            return null;
        });

        // Transação anônima sem app.current_tenant configurado
        transactionTemplate.execute(status -> {
            List<String> products = jdbcTemplate.query(
                    "SELECT title FROM products WHERE id = ?",
                    (rs, rowNum) -> rs.getString("title"),
                    alphaProductId
            );
            assertThat(products).isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("RLS: Tentativa de UPDATE cross-tenant afeta 0 linhas")
    void testCrossTenantUpdateBlockedByRls() {
        UUID alphaProductId = UUID.randomUUID();

        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);
            Product product = new Product(
                    alphaProductId,
                    tenantAlphaId,
                    "Teclado Original",
                    "KB-ORIG-01",
                    "BrandA",
                    "Periféricos",
                    "ACTIVE"
            );
            productRepository.save(product);
            return null;
        });

        // Tenant Beta tenta alterar o produto do Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            int rowsUpdated = jdbcTemplate.update(
                    "UPDATE products SET title = 'Teclado Hackeado' WHERE id = ?",
                    alphaProductId
            );
            assertThat(rowsUpdated).isEqualTo(0);
            return null;
        });

        // Verifica que o dado permanece inalterado no Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);
            Product original = productRepository.findById(alphaProductId).orElseThrow();
            assertThat(original.getTitle()).isEqualTo("Teclado Original");
            return null;
        });
    }

    @Test
    @DisplayName("RLS: Tentativa de DELETE cross-tenant afeta 0 linhas")
    void testCrossTenantDeleteBlockedByRls() {
        UUID alphaProductId = UUID.randomUUID();

        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);
            Product product = new Product(
                    alphaProductId,
                    tenantAlphaId,
                    "Mouse Original",
                    "MS-ORIG-01",
                    "BrandA",
                    "Periféricos",
                    "ACTIVE"
            );
            productRepository.save(product);
            return null;
        });

        // Tenant Beta tenta deletar o produto do Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            int rowsDeleted = jdbcTemplate.update(
                    "DELETE FROM products WHERE id = ?",
                    alphaProductId
            );
            assertThat(rowsDeleted).isEqualTo(0);
            return null;
        });

        // Verifica que o registro ainda existe intacto no Tenant Alpha
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);
            assertThat(productRepository.findById(alphaProductId)).isPresent();
            return null;
        });
    }

    @Test
    @DisplayName("RLS: Isolamento estrito em Checkpoint e ExternalIdentifierMapping")
    void testCheckpointsAndMappingsIsolatedByRls() {
        UUID alphaAccountId = UUID.randomUUID();
        UUID alphaCheckpointId = UUID.randomUUID();
        UUID alphaMappingId = UUID.randomUUID();
        UUID alphaProductId = UUID.randomUUID();

        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantAlphaId);

            SellerAccount account = new SellerAccount(
                    alphaAccountId,
                    tenantAlphaId,
                    "SHOPEE",
                    "SHOP_ALPHA_01",
                    "Loja Shopee Alpha",
                    "ACTIVE"
            );
            sellerAccountRepository.save(account);

            ListingSyncCheckpoint checkpoint = new ListingSyncCheckpoint(
                    alphaCheckpointId,
                    tenantAlphaId,
                    alphaAccountId,
                    "SHOPEE",
                    50
            );
            checkpoint.setTotalItemsSynced(150);
            checkpoint.setSyncStatus("COMPLETED");
            checkpoint.setCompletedAt(Instant.now());
            checkpointRepository.save(checkpoint);

            ExternalIdentifierMapping mapping = new ExternalIdentifierMapping(
                    alphaMappingId,
                    tenantAlphaId,
                    "SHOPEE",
                    "ITEM_ID",
                    "SHP-ITEM-888",
                    "PRODUCT",
                    alphaProductId
            );
            externalMappingRepository.save(mapping);
            return null;
        });

        // Tenant Beta não consegue enxergar nem checkpoint nem mapeamento
        transactionTemplate.execute(status -> {
            tenantDatabaseSession.setTenantSession(tenantBetaId);

            assertThat(checkpointRepository.findById(alphaCheckpointId)).isEmpty();
            assertThat(externalMappingRepository.findById(alphaMappingId)).isEmpty();

            List<String> rawCheckpoints = jdbcTemplate.query(
                    "SELECT sync_status FROM listing_sync_checkpoints WHERE id = ?",
                    (rs, rowNum) -> rs.getString("sync_status"),
                    alphaCheckpointId
            );
            assertThat(rawCheckpoints).isEmpty();

            List<String> rawMappings = jdbcTemplate.query(
                    "SELECT external_id_value FROM external_identifier_mappings WHERE id = ?",
                    (rs, rowNum) -> rs.getString("external_id_value"),
                    alphaMappingId
            );
            assertThat(rawMappings).isEmpty();

            return null;
        });
    }
}
