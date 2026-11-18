package com.sellerintelligence.modules.orderingestion;

import com.sellerintelligence.core.security.JwtTokenProvider;
import com.sellerintelligence.modules.catalog.SellerAccount;
import com.sellerintelligence.modules.catalog.SellerAccountRepository;
import com.sellerintelligence.modules.orderingestion.model.OrderStatus;
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
class OrderControllerTest {

    private static final UUID TENANT_ALPHA_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_BETA_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private SellerAccountRepository sellerAccountRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderSyncCheckpointRepository checkpointRepository;

    @Autowired
    private OrderDlqRecordRepository dlqRecordRepository;

    private String tokenAlpha;
    private String tokenBeta;
    private SellerAccount accountMeli;
    private Order orderAlpha;
    private OrderItem orderItemAlpha;

    @BeforeEach
    void setUp() {
        tokenAlpha = jwtTokenProvider.generateToken(USER_ID, "alpha@example.com", TENANT_ALPHA_ID, List.of("ADMIN"));
        tokenBeta = jwtTokenProvider.generateToken(USER_ID, "beta@example.com", TENANT_BETA_ID, List.of("ADMIN"));

        accountMeli = sellerAccountRepository.save(new SellerAccount(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                "MERCADO_LIVRE",
                "MELI_ORDERS_ACC_01",
                "Loja Oficial Mercado Livre",
                "ACTIVE"
        ));

        orderAlpha = orderRepository.save(new Order(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                accountMeli.getId(),
                "MERCADO_LIVRE",
                "ORD-ALPHA-1234",
                OrderStatus.PAID,
                new BigDecimal("199.90"),
                new BigDecimal("15.00"),
                new BigDecimal("214.90"),
                "BRL",
                "Carlos Silva",
                "***.456.789-**",
                "BR987654321XP",
                null,
                null,
                null,
                "{\"id\":\"ORD-ALPHA-1234\"}",
                "v1",
                "MERCADO_LIVRE",
                Instant.now().minusSeconds(1800),
                Instant.now()
        ));

        orderItemAlpha = orderItemRepository.save(new OrderItem(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                orderAlpha.getId(),
                null,
                null,
                "MLB-LISTING-99",
                "ITEM-EXT-01",
                "SKU-TECLADO",
                "Teclado Mecânico Compacto RGB",
                new BigDecimal("199.90"),
                1,
                new BigDecimal("199.90")
        ));
    }

    @Test
    @DisplayName("1. Retorna lista paginada de pedidos para o tenant autenticado")
    void shouldReturnPaginatedOrdersForActiveTenant() throws Exception {
        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].externalOrderId", is("ORD-ALPHA-1234")))
                .andExpect(jsonPath("$.content[0].status", is("PAID")))
                .andExpect(jsonPath("$.content[0].totalAmount", is(214.9000)))
                .andExpect(jsonPath("$.content[0].items", hasSize(1)))
                .andExpect(jsonPath("$.content[0].items[0].title", is("Teclado Mecânico Compacto RGB")))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.page", is(0)));
    }

    @Test
    @DisplayName("2. Filtra pedidos por marketplace, status e termo de busca")
    void shouldFilterOrdersByMarketplaceStatusAndSearch() throws Exception {
        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .param("marketplace", "MERCADO_LIVRE")
                        .param("status", "PAID")
                        .param("search", "ORD-ALPHA")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].externalOrderId", is("ORD-ALPHA-1234")));

        // Busca por status inexistente retorna vazio
        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .param("status", "CANCELLED")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("3. Retorna detalhe de pedido por ID com itens associados")
    void shouldReturnOrderDetailById() throws Exception {
        mockMvc.perform(get("/api/v1/orders/" + orderAlpha.getId())
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(orderAlpha.getId().toString())))
                .andExpect(jsonPath("$.externalOrderId", is("ORD-ALPHA-1234")))
                .andExpect(jsonPath("$.buyerName", is("Carlos Silva")))
                .andExpect(jsonPath("$.trackingNumber", is("BR987654321XP")))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].sku", is("SKU-TECLADO")));
    }

    @Test
    @DisplayName("4. Retorna HTTP 404 quando Tenant Beta tenta acessar pedido do Tenant Alpha (Zero vazamento cross-tenant)")
    void shouldReturn404WhenTenantBetaAttemptsToAccessTenantAlphaOrder() throws Exception {
        mockMvc.perform(get("/api/v1/orders/" + orderAlpha.getId())
                        .header("Authorization", "Bearer " + tokenBeta)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("5. Retorna checkpoints e registros de DLQ para o tenant ativo")
    void shouldReturnCheckpointsAndDlqRecordsForActiveTenant() throws Exception {
        checkpointRepository.save(new OrderSyncCheckpoint(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                accountMeli.getId(),
                "MERCADO_LIVRE",
                "COMPLETED",
                "cursor-xyz",
                10,
                50,
                10
        ));

        dlqRecordRepository.save(new OrderDlqRecord(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                accountMeli.getId(),
                "MERCADO_LIVRE",
                "ORD-CORRUPTED-999",
                "{}",
                "Erro de validação de teste"
        ));

        mockMvc.perform(get("/api/v1/orders/checkpoints")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].marketplace", is("MERCADO_LIVRE")));

        mockMvc.perform(get("/api/v1/orders/dlq")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].errorReason", is("Erro de validação de teste")));
    }
}
