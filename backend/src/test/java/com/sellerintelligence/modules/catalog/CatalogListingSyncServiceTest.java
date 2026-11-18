package com.sellerintelligence.modules.catalog;

import com.sellerintelligence.modules.catalog.model.ListingPageRequest;
import com.sellerintelligence.modules.catalog.model.ListingPageResponse;
import com.sellerintelligence.modules.catalog.model.ListingSyncResult;
import com.sellerintelligence.modules.catalog.model.RawListingItem;
import com.sellerintelligence.modules.catalog.service.CatalogListingSyncService;
import com.sellerintelligence.modules.catalog.service.CatalogService;
import com.sellerintelligence.modules.marketplaceadapters.fake.FakeMarketplaceAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CatalogListingSyncServiceTest {

    @Autowired
    private CatalogListingSyncService syncService;

    @Autowired
    private CatalogService catalogService;

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

    private UUID tenant1;
    private UUID tenant2;
    private SellerAccount accountMeli;
    private SellerAccount accountShopee;
    private FakeMarketplaceAdapter fakeAdapter;

    @BeforeEach
    void setUp() {
        tenant1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
        tenant2 = UUID.fromString("22222222-2222-2222-2222-222222222222");

        accountMeli = catalogService.registerSellerAccount(
                tenant1,
                "MERCADO_LIVRE",
                "SELLER_MELI_01",
                "Loja Oficial Mercado Livre"
        );

        accountShopee = catalogService.registerSellerAccount(
                tenant1,
                "SHOPEE",
                "SHOP_SHOPEE_01",
                "Loja Oficial Shopee"
        );

        fakeAdapter = FakeMarketplaceAdapter.createDefault();
    }

    @Test
    @DisplayName("1. Sincroniza anúncios próprios com paginação e persiste payload minimizado e versionado")
    void shouldSyncOwnListingsWithPaginationAndPayloadPreservation() {
        // Simula 3 itens em 2 páginas (página 1 com 2 itens, página 2 com 1 item)
        RawListingItem item1 = new RawListingItem(
                "MLB-001",
                "Mouse Gamer RGB 16000 DPI",
                new BigDecimal("149.90"),
                50,
                "ACTIVE",
                "https://mercadolivre.com.br/MLB-001",
                "SKU-MOUSE-RGB",
                "7891234567890",
                "{\"id\":\"MLB-001\",\"price\":149.90,\"status\":\"active\"}",
                "MERCADO_LIVRE_ITEMS_API",
                "v1"
        );

        RawListingItem item2 = new RawListingItem(
                "MLB-002",
                "Teclado Mecânico Switch Blue",
                new BigDecimal("299.00"),
                20,
                "ACTIVE",
                "https://mercadolivre.com.br/MLB-002",
                "SKU-KB-BLUE",
                "7891234567891",
                "{\"id\":\"MLB-002\",\"price\":299.00,\"status\":\"active\"}",
                "MERCADO_LIVRE_ITEMS_API",
                "v1"
        );

        RawListingItem item3 = new RawListingItem(
                "MLB-003",
                "Headset Surround 7.1",
                new BigDecimal("199.50"),
                15,
                "ACTIVE",
                "https://mercadolivre.com.br/MLB-003",
                "SKU-HS-71",
                "7891234567892",
                "{\"id\":\"MLB-003\",\"price\":199.50,\"status\":\"active\"}",
                "MERCADO_LIVRE_ITEMS_API",
                "v1"
        );

        ListingSyncResult result = syncService.syncOwnListings(
                tenant1,
                accountMeli.getId(),
                2,
                fakeAdapter,
                (req, ctx) -> {
                    if (req.offset() == 0) {
                        return new ListingPageResponse(List.of(item1, item2), 0, 2, 3, true);
                    } else if (req.offset() == 2) {
                        return new ListingPageResponse(List.of(item3), 2, 2, 3, false);
                    }
                    return new ListingPageResponse(List.of(), req.offset(), 2, 3, false);
                }
        );

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.syncedCount()).isEqualTo(3);
        assertThat(result.createdCount()).isEqualTo(3);
        assertThat(result.updatedCount()).isEqualTo(0);

        List<MarketplaceListing> saved = listingRepository.findAllByTenantId(tenant1);
        assertThat(saved).hasSize(3);

        MarketplaceListing savedItem1 = saved.stream()
                .filter(l -> l.getExternalListingId().equals("MLB-001"))
                .findFirst()
                .orElseThrow();

        assertThat(savedItem1.getTitle()).isEqualTo("Mouse Gamer RGB 16000 DPI");
        assertThat(savedItem1.getPrice()).isEqualByComparingTo(new BigDecimal("149.90"));
        assertThat(savedItem1.getAvailableQuantity()).isEqualTo(50);
        assertThat(savedItem1.getRawPayloadJson()).isEqualTo("{\"id\":\"MLB-001\",\"price\":149.90,\"status\":\"active\"}");
        assertThat(savedItem1.getSchemaVersion()).isEqualTo("v1");
        assertThat(savedItem1.getOrigin()).isEqualTo("MERCADO_LIVRE_ITEMS_API");
        assertThat(savedItem1.getSyncedAt()).isNotNull();

        // Checkpoint foi completado
        Optional<ListingSyncCheckpoint> chkOpt = checkpointRepository
                .findByTenantIdAndSellerAccountIdAndMarketplace(tenant1, accountMeli.getId(), "MERCADO_LIVRE");
        assertThat(chkOpt).isPresent();
        assertThat(chkOpt.get().getSyncStatus()).isEqualTo("COMPLETED");
        assertThat(chkOpt.get().getTotalItemsSynced()).isEqualTo(3);
    }

    @Test
    @DisplayName("2. Idempotência: Reprocessar a mesma página não duplica registros e reconcilia dados atualizados")
    void shouldBeIdempotentWhenReprocessingSamePage() {
        RawListingItem initialItem = new RawListingItem(
                "SHP-101",
                "Cabo USB-C Trançado 2m",
                new BigDecimal("29.90"),
                100,
                "ACTIVE",
                "https://shopee.com.br/product/SHP-101",
                "SKU-CABLE-01",
                "7890000000001",
                "{\"item_id\":101,\"price\":29.90}",
                "SHOPEE_OPENAPI_V2",
                "v1"
        );

        // 1ª execução
        ListingSyncResult firstResult = syncService.syncOwnListings(
                tenant1,
                accountShopee.getId(),
                10,
                fakeAdapter,
                (req, ctx) -> new ListingPageResponse(List.of(initialItem), 0, 10, 1, false)
        );

        assertThat(firstResult.createdCount()).isEqualTo(1);
        assertThat(firstResult.updatedCount()).isEqualTo(0);
        assertThat(listingRepository.countByTenantId(tenant1)).isEqualTo(1);

        // 2ª execução com os mesmos itens porém preço e estoque alterados no canal (reconciliação)
        RawListingItem updatedItem = new RawListingItem(
                "SHP-101",
                "Cabo USB-C Trançado 2m Premium",
                new BigDecimal("34.90"),
                85,
                "ACTIVE",
                "https://shopee.com.br/product/SHP-101",
                "SKU-CABLE-01",
                "7890000000001",
                "{\"item_id\":101,\"price\":34.90}",
                "SHOPEE_OPENAPI_V2",
                "v1"
        );

        ListingSyncResult secondResult = syncService.syncOwnListings(
                tenant1,
                accountShopee.getId(),
                10,
                fakeAdapter,
                (req, ctx) -> new ListingPageResponse(List.of(updatedItem), 0, 10, 1, false)
        );

        // Idempotência comprovada: zero duplicações!
        assertThat(secondResult.createdCount()).isEqualTo(0);
        assertThat(secondResult.updatedCount()).isEqualTo(1);
        assertThat(listingRepository.countByTenantId(tenant1)).isEqualTo(1);

        MarketplaceListing reconciled = listingRepository.findAllByTenantId(tenant1).get(0);
        assertThat(reconciled.getTitle()).isEqualTo("Cabo USB-C Trançado 2m Premium");
        assertThat(reconciled.getPrice()).isEqualByComparingTo(new BigDecimal("34.90"));
        assertThat(reconciled.getAvailableQuantity()).isEqualTo(85);
    }

    @Test
    @DisplayName("3. Separação de Entidades: Um produto canônico pode ter múltiplos anúncios e não deduplica apenas por título")
    void shouldAllowOneCanonicalProductWithMultipleListingsAndNeverDeduplicateByTitleAlone() {
        // Cria previamente um produto canônico com SKU mestre
        Product canonicalProduct = catalogService.createProduct(
                tenant1,
                "Monitor Gamer 27 144Hz",
                "MONITOR-27-144",
                "UltraView",
                "Monitores"
        );

        // Cenário A: Dois anúncios no Mercado Livre com o mesmo SKU vinculam ao mesmo produto canônico (1:N)
        RawListingItem listingClasico = new RawListingItem(
                "MLB-901",
                "Monitor Gamer 27 Pol 144Hz - Envio Rápido",
                new BigDecimal("1299.00"),
                10,
                "ACTIVE",
                "https://mercadolivre.com.br/MLB-901",
                "MONITOR-27-144",
                null,
                "{\"id\":\"MLB-901\"}",
                "MERCADO_LIVRE_ITEMS_API",
                "v1"
        );

        RawListingItem listingPremium = new RawListingItem(
                "MLB-902",
                "Monitor Gamer 27 Pol 144Hz - Em 12x Sem Juros",
                new BigDecimal("1449.00"),
                10,
                "ACTIVE",
                "https://mercadolivre.com.br/MLB-902",
                "MONITOR-27-144",
                null,
                "{\"id\":\"MLB-902\"}",
                "MERCADO_LIVRE_ITEMS_API",
                "v1"
        );

        // Cenário B: Terceiro anúncio com o MESMO título porém sem SKU ou com SKU diferente
        // Pela regra inegociável do PRD: "não deduplicar apenas por título"!
        RawListingItem listingDifferentProduct = new RawListingItem(
                "MLB-903",
                "Monitor Gamer 27 144Hz", // Título idêntico ao produto canônico
                new BigDecimal("1199.00"),
                5,
                "ACTIVE",
                "https://mercadolivre.com.br/MLB-903",
                "MONITOR-OUTRO-SKU", // SKU diferente
                null,
                "{\"id\":\"MLB-903\"}",
                "MERCADO_LIVRE_ITEMS_API",
                "v1"
        );

        syncService.syncOwnListings(
                tenant1,
                accountMeli.getId(),
                10,
                fakeAdapter,
                (req, ctx) -> new ListingPageResponse(
                        List.of(listingClasico, listingPremium, listingDifferentProduct),
                        0,
                        10,
                        3,
                        false
                )
        );

        List<MarketplaceListing> allListings = listingRepository.findAllByTenantId(tenant1);
        assertThat(allListings).hasSize(3);

        // Os dois anúncios com o mesmo SKU apontam para o mesmo produto canônico (1:N)
        List<MarketplaceListing> listingsOfCanonical = listingRepository
                .findAllByTenantIdAndProductId(tenant1, canonicalProduct.getId());
        assertThat(listingsOfCanonical).hasSize(2);
        assertThat(listingsOfCanonical)
                .extracting(MarketplaceListing::getExternalListingId)
                .containsExactlyInAnyOrder("MLB-901", "MLB-902");

        // O anúncio com SKU diferente criou e vinculou a um produto diferente, NÃO fundiu pelo título!
        MarketplaceListing separateListing = listingRepository
                .findByTenantIdAndMarketplaceAndExternalListingId(tenant1, "MERCADO_LIVRE", "MLB-903")
                .orElseThrow();
        assertThat(separateListing.getProductId()).isNotEqualTo(canonicalProduct.getId());

        Product distinctProduct = productRepository.findById(separateListing.getProductId()).orElseThrow();
        assertThat(distinctProduct.getCanonicalSku()).isEqualTo("MONITOR-OUTRO-SKU");
    }

    @Test
    @DisplayName("4. Retomada de falha parcial: Checkpoint permite continuar a sincronização a partir da página interrompida")
    void shouldResumeFromCheckpointAfterPartialFailure() {
        AtomicInteger callCount = new AtomicInteger(0);

        RawListingItem itemPage1 = new RawListingItem(
                "SHP-P1",
                "Item Página 1",
                new BigDecimal("10.00"),
                10,
                "ACTIVE",
                "https://shopee.com/P1",
                "SKU-P1",
                null,
                "{}",
                "SHOPEE_OPENAPI_V2",
                "v1"
        );

        RawListingItem itemPage2 = new RawListingItem(
                "SHP-P2",
                "Item Página 2",
                new BigDecimal("20.00"),
                20,
                "ACTIVE",
                "https://shopee.com/P2",
                "SKU-P2",
                null,
                "{}",
                "SHOPEE_OPENAPI_V2",
                "v1"
        );

        // 1ª tentativa: Falha na página 2 (offset 1)
        assertThatThrownBy(() -> {
            syncService.syncOwnListings(
                    tenant1,
                    accountShopee.getId(),
                    1,
                    fakeAdapter,
                    (req, ctx) -> {
                        callCount.incrementAndGet();
                        if (req.offset() == 0) {
                            return new ListingPageResponse(List.of(itemPage1), 0, 1, 2, true);
                        } else {
                            throw new RuntimeException("Simulação de falha temporária de rede na página 2");
                        }
                    }
            );
        }).isInstanceOf(CatalogSyncException.class);

        // Verifica que o item da página 1 foi gravado e o checkpoint registrado com status FAILED no offset 1
        assertThat(listingRepository.countByTenantId(tenant1)).isEqualTo(1);
        ListingSyncCheckpoint checkpoint = checkpointRepository
                .findByTenantIdAndSellerAccountIdAndMarketplace(tenant1, accountShopee.getId(), "SHOPEE")
                .orElseThrow();
        assertThat(checkpoint.getSyncStatus()).isEqualTo("FAILED");
        assertThat(checkpoint.getLastOffset()).isEqualTo(1);
        assertThat(checkpoint.getErrorMessage()).contains("Simulação de falha temporária");

        // 2ª tentativa: Retomada segura. O pageFetcher deve receber offset=1 diretamente!
        List<Integer> requestedOffsets = new ArrayList<>();

        ListingSyncResult resumedResult = syncService.syncOwnListings(
                tenant1,
                accountShopee.getId(),
                1,
                fakeAdapter,
                (req, ctx) -> {
                    requestedOffsets.add(req.offset());
                    if (req.offset() == 1) {
                        return new ListingPageResponse(List.of(itemPage2), 1, 1, 2, false);
                    }
                    return new ListingPageResponse(List.of(), req.offset(), 1, 2, false);
                }
        );

        assertThat(resumedResult.status()).isEqualTo("COMPLETED");
        // O primeiro offset requisitado na retomada foi exatamente o offset 1 (retomou de onde parou!)
        assertThat(requestedOffsets).contains(1);
        assertThat(requestedOffsets).doesNotContain(0);

        // Total de 2 itens gravados, zero duplicatas
        assertThat(listingRepository.countByTenantId(tenant1)).isEqualTo(2);
        ListingSyncCheckpoint completedCheckpoint = checkpointRepository
                .findByTenantIdAndSellerAccountIdAndMarketplace(tenant1, accountShopee.getId(), "SHOPEE")
                .orElseThrow();
        assertThat(completedCheckpoint.getSyncStatus()).isEqualTo("COMPLETED");
        assertThat(completedCheckpoint.getLastOffset()).isEqualTo(2);
    }

    @Test
    @DisplayName("5. Isolamento Multi-Tenant: Dados de dois tenants são estritamente isolados em produtos, listings e checkpoints")
    void shouldEnsureStrictMultiTenantIsolation() {
        SellerAccount tenant2Account = catalogService.registerSellerAccount(
                tenant2,
                "MERCADO_LIVRE",
                "SELLER_MELI_T2",
                "Loja Tenant 2"
        );

        RawListingItem itemTenant1 = new RawListingItem(
                "MLB-T1-100",
                "Produto Exclusivo Tenant 1",
                new BigDecimal("99.00"),
                10,
                "ACTIVE",
                "https://mercadolivre.com/T1",
                "SKU-T1",
                null,
                "{}",
                "MERCADO_LIVRE_ITEMS_API",
                "v1"
        );

        RawListingItem itemTenant2 = new RawListingItem(
                "MLB-T2-200",
                "Produto Exclusivo Tenant 2",
                new BigDecimal("199.00"),
                20,
                "ACTIVE",
                "https://mercadolivre.com/T2",
                "SKU-T2",
                null,
                "{}",
                "MERCADO_LIVRE_ITEMS_API",
                "v1"
        );

        // Sincroniza Tenant 1
        syncService.syncOwnListings(
                tenant1,
                accountMeli.getId(),
                10,
                fakeAdapter,
                (req, ctx) -> new ListingPageResponse(List.of(itemTenant1), 0, 10, 1, false)
        );

        // Sincroniza Tenant 2
        syncService.syncOwnListings(
                tenant2,
                tenant2Account.getId(),
                10,
                fakeAdapter,
                (req, ctx) -> new ListingPageResponse(List.of(itemTenant2), 0, 10, 1, false)
        );

        // Consultas pelo Tenant 1 retornam exclusivamente itens do Tenant 1
        List<MarketplaceListing> listingsT1 = listingRepository.findAllByTenantId(tenant1);
        assertThat(listingsT1).hasSize(1);
        assertThat(listingsT1.get(0).getExternalListingId()).isEqualTo("MLB-T1-100");

        List<Product> productsT1 = productRepository.findAllByTenantId(tenant1);
        assertThat(productsT1).hasSize(1);
        assertThat(productsT1.get(0).getTitle()).isEqualTo("Produto Exclusivo Tenant 1");

        // Consultas pelo Tenant 2 retornam exclusivamente itens do Tenant 2
        List<MarketplaceListing> listingsT2 = listingRepository.findAllByTenantId(tenant2);
        assertThat(listingsT2).hasSize(1);
        assertThat(listingsT2.get(0).getExternalListingId()).isEqualTo("MLB-T2-200");

        List<Product> productsT2 = productRepository.findAllByTenantId(tenant2);
        assertThat(productsT2).hasSize(1);
        assertThat(productsT2.get(0).getTitle()).isEqualTo("Produto Exclusivo Tenant 2");

        // Tentativa de sincronizar a conta do Tenant 2 informando Tenant 1 deve falhar
        assertThatThrownBy(() -> {
            syncService.syncOwnListings(
                    tenant1, // Tenant errado
                    tenant2Account.getId(),
                    10,
                    fakeAdapter,
                    (req, ctx) -> new ListingPageResponse(List.of(), 0, 10, 0, false)
            );
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Conta de vendedor não encontrada para o tenant informado");
    }

    @Test
    @DisplayName("6. Mapeamento explícito de identificador externo: ITEM_ID vincula com sucesso ao produto canônico")
    void shouldLinkListingUsingExplicitExternalIdentifierMapping() {
        Product canonicalMaster = catalogService.createProduct(
                tenant1,
                "Smartphone Flagship X",
                "PHONE-X-01",
                "BrandX",
                "Smartphones"
        );

        // Cria mapeamento prévio de ITEM_ID para o produto
        externalMappingRepository.save(new ExternalIdentifierMapping(
                UUID.randomUUID(),
                tenant1,
                "MERCADO_LIVRE",
                "ITEM_ID",
                "MLB-PHONE-007",
                "PRODUCT",
                canonicalMaster.getId()
        ));

        // Ingestão de anúncio cujo ID externo bate com o mapeamento
        RawListingItem listingItem = new RawListingItem(
                "MLB-PHONE-007",
                "Smartphone Flagship X 256GB - Pronta Entrega",
                new BigDecimal("4999.00"),
                8,
                "ACTIVE",
                "https://mercadolivre.com/phone",
                null, // Sem SKU no anúncio
                null,
                "{}",
                "MERCADO_LIVRE_ITEMS_API",
                "v1"
        );

        syncService.syncOwnListings(
                tenant1,
                accountMeli.getId(),
                10,
                fakeAdapter,
                (req, ctx) -> new ListingPageResponse(List.of(listingItem), 0, 10, 1, false)
        );

        MarketplaceListing saved = listingRepository
                .findByTenantIdAndMarketplaceAndExternalListingId(tenant1, "MERCADO_LIVRE", "MLB-PHONE-007")
                .orElseThrow();

        // O anúncio foi vinculado com precisão ao produto canônico mapeado
        assertThat(saved.getProductId()).isEqualTo(canonicalMaster.getId());
    }
}
