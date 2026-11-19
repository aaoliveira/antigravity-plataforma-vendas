package com.sellerintelligence.modules.financialanalytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.core.security.JwtTokenProvider;
import com.sellerintelligence.modules.catalog.SellerAccount;
import com.sellerintelligence.modules.catalog.SellerAccountRepository;
import com.sellerintelligence.modules.financialanalytics.model.ReconcileSettlementCommand;
import com.sellerintelligence.modules.orderingestion.Order;
import com.sellerintelligence.modules.orderingestion.OrderItem;
import com.sellerintelligence.modules.orderingestion.OrderItemRepository;
import com.sellerintelligence.modules.orderingestion.OrderRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderFinancialFactControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private SellerAccountRepository sellerAccountRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private String token;
    private SellerAccount sellerAccount;
    private Order order;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        token = jwtTokenProvider.generateToken(
                USER_ID, "financial-tester@sellerintelligence.com", TENANT_ID, List.of("ROLE_ADMIN")
        );

        sellerAccount = sellerAccountRepository.save(new SellerAccount(
                UUID.randomUUID(),
                TENANT_ID,
                "MERCADO_LIVRE",
                "MLB-ACC-TEST",
                "Loja Teste",
                "ACTIVE"
        ));

        order = orderRepository.save(new Order(
                UUID.randomUUID(),
                TENANT_ID,
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                "ORDER-EXT-001",
                OrderStatus.PAID,
                new BigDecimal("200.0000"),
                new BigDecimal("18.0000"),
                new BigDecimal("218.0000"),
                "BRL",
                "Cliente Teste",
                "12345678900",
                "TRACK123",
                null,
                null,
                null,
                "{}",
                "v1",
                "MERCADO_LIVRE_API",
                Instant.now(),
                Instant.now()
        ));

        orderItem = orderItemRepository.save(new OrderItem(
                UUID.randomUUID(),
                TENANT_ID,
                order.getId(),
                null,
                null,
                "MLB-LST-1",
                "MLB-ITM-1",
                "SKU-FIN-01",
                "Item Financeiro Teste",
                new BigDecimal("200.0000"),
                1,
                new BigDecimal("200.0000")
        ));
    }

    @Test
    @DisplayName("1. Fluxo REST completo: apurar fatos, consultar por ID, reconciliar extrato externo e consultar resumo")
    void shouldExecuteFullFinancialFactsFlow() throws Exception {
        // 1. POST /api/v1/financial-facts/orders/{orderId}/compute
        mockMvc.perform(post("/api/v1/financial-facts/orders/{orderId}/compute", order.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(order.getId().toString()))
                .andExpect(jsonPath("$.grossRevenue").value(200.0000))
                .andExpect(jsonPath("$.shippingCost").value(18.0000))
                .andExpect(jsonPath("$.reconciliationStatus").value("ESTIMATED"))
                .andExpect(jsonPath("$.antiHallucinationDisclaimer", containsString("Margem de Contribuição")))
                .andExpect(jsonPath("$.antiHallucinationDisclaimer", containsString("proibido rotular este valor como Lucro Líquido")))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].sku").value("SKU-FIN-01"));

        // 2. GET /api/v1/financial-facts/orders/{orderId}
        mockMvc.perform(get("/api/v1/financial-facts/orders/{orderId}", order.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(order.getId().toString()))
                .andExpect(jsonPath("$.netRevenue").value(200.0000));

        // 3. POST /api/v1/financial-facts/orders/{orderId}/reconcile
        ReconcileSettlementCommand reconcileCmd = new ReconcileSettlementCommand(
                "EXT-SETTLE-REST-01",
                Instant.now(),
                new BigDecimal("28.0000"),
                new BigDecimal("6.0000"),
                new BigDecimal("18.0000"),
                new BigDecimal("148.0000"),
                "Conciliado com extrato Mercado Pago"
        );

        mockMvc.perform(post("/api/v1/financial-facts/orders/{orderId}/reconcile", order.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reconcileCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settlementExternalId").value("EXT-SETTLE-REST-01"))
                .andExpect(jsonPath("$.feeProvenance").value("CONFIRMED"))
                .andExpect(jsonPath("$.shippingProvenance").value("CONFIRMED"))
                .andExpect(jsonPath("$.marketplaceFee").value(28.0000))
                .andExpect(jsonPath("$.fixedFee").value(6.0000));

        // 4. GET /api/v1/financial-facts (listagem filtrada)
        mockMvc.perform(get("/api/v1/financial-facts")
                        .header("Authorization", "Bearer " + token)
                        .param("marketplace", "MERCADO_LIVRE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].orderId").value(order.getId().toString()));

        // 5. GET /api/v1/financial-facts/summary
        mockMvc.perform(get("/api/v1/financial-facts/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalGrossRevenue", greaterThan(0.0)))
                .andExpect(jsonPath("$.antiHallucinationDisclaimer", containsString("Margem de Contribuição")));
    }

    @Test
    @DisplayName("2. Deve retornar 404 ao tentar apurar fatos para pedido inexistente")
    void shouldReturn404ForNonExistentOrder() throws Exception {
        UUID fakeOrderId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/financial-facts/orders/{orderId}/compute", fakeOrderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
