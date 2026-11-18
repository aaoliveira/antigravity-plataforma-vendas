package com.sellerintelligence.modules.catalog;

import com.sellerintelligence.core.security.JwtTokenProvider;
import com.sellerintelligence.modules.catalog.service.CatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogControllerTest {

    private static final UUID TENANT_ALPHA_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_BETA_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private MarketplaceListingRepository listingRepository;

    private String tokenAlpha;
    private String tokenBeta;
    private SellerAccount accountMeli;
    private SellerAccount accountShopee;
    private Product product1;
    private MarketplaceListing listing1;

    @BeforeEach
    void setUp() {
        tokenAlpha = jwtTokenProvider.generateToken(USER_ID, "alpha@example.com", TENANT_ALPHA_ID, List.of("ADMIN"));
        tokenBeta = jwtTokenProvider.generateToken(USER_ID, "beta@example.com", TENANT_BETA_ID, List.of("ADMIN"));

        accountMeli = catalogService.registerSellerAccount(
                TENANT_ALPHA_ID,
                "MERCADO_LIVRE",
                "SELLER_MELI_01",
                "Loja Oficial Mercado Livre Alpha"
        );

        accountShopee = catalogService.registerSellerAccount(
                TENANT_ALPHA_ID,
                "SHOPEE",
                "SHOP_SHOPEE_01",
                "Loja Oficial Shopee Alpha"
        );

        product1 = catalogService.createProduct(
                TENANT_ALPHA_ID,
                "Mouse Gamer Ergonômico Canônico",
                "SKU-MOUSE-CANONICAL",
                "Logitech",
                "Periféricos"
        );

        listing1 = new MarketplaceListing(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                accountMeli.getId(),
                product1.getId(),
                null,
                "MERCADO_LIVRE",
                "MLB-999111",
                "SKU-MOUSE-CANONICAL",
                null,
                "Mouse Gamer RGB 16000 DPI Sensor Óptico",
                new BigDecimal("149.90"),
                "BRL",
                50,
                "ACTIVE",
                "https://mercadolivre.com.br/MLB-999111",
                "{\"id\":\"MLB-999111\"}",
                "v1",
                "MERCADO_LIVRE_ITEMS_API"
        );
        listingRepository.save(listing1);

        MarketplaceListing listing2 = new MarketplaceListing(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                accountShopee.getId(),
                product1.getId(),
                null,
                "SHOPEE",
                "SHP-555222",
                "SKU-MOUSE-CANONICAL",
                null,
                "Mouse Gamer Shopee Sensor Óptico",
                new BigDecimal("139.90"),
                "BRL",
                20,
                "PAUSED",
                "https://shopee.com.br/product/SHP-555222",
                "{\"item_id\":\"555222\"}",
                "v1",
                "SHOPEE_OPENAPI_V2"
        );
        listingRepository.save(listing2);
    }

    @Test
    @DisplayName("GET /api/v1/catalog/listings: retorna lista paginada e filtrada para o tenant ativo")
    void shouldReturnPaginatedListingsForActiveTenant() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/listings")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(10)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].product.title", is("Mouse Gamer Ergonômico Canônico")));
    }

    @Test
    @DisplayName("GET /api/v1/catalog/listings: filtros combinados por marketplace, status e busca")
    void shouldFilterListingsByMarketplaceAndStatusAndSearch() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/listings")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .param("marketplace", "MERCADO_LIVRE")
                        .param("status", "ACTIVE")
                        .param("search", "16000 DPI")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].externalListingId", is("MLB-999111")))
                .andExpect(jsonPath("$.content[0].marketplace", is("MERCADO_LIVRE")))
                .andExpect(jsonPath("$.content[0].status", is("ACTIVE")));
    }

    @Test
    @DisplayName("GET /api/v1/catalog/listings/{id}: detalhe de anúncio por ID")
    void shouldReturnListingDetailById() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/listings/" + listing1.getId())
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(listing1.getId().toString())))
                .andExpect(jsonPath("$.title", is("Mouse Gamer RGB 16000 DPI Sensor Óptico")))
                .andExpect(jsonPath("$.sellerAccountName", is("Loja Oficial Mercado Livre Alpha")))
                .andExpect(jsonPath("$.product.canonicalSku", is("SKU-MOUSE-CANONICAL")));
    }

    @Test
    @DisplayName("GET /api/v1/catalog/listings/{id}: tentativa cross-tenant retorna 404 (ResourceNotFoundException)")
    void shouldReturn404WhenTenantBetaAttemptsToAccessTenantAlphaListing() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/listings/" + listing1.getId())
                        .header("Authorization", "Bearer " + tokenBeta)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/catalog/seller-accounts: retorna contas do tenant ativo")
    void shouldReturnSellerAccountsForActiveTenant() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/seller-accounts")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].marketplace", containsInAnyOrder("MERCADO_LIVRE", "SHOPEE")));
    }
}
