package br.com.sellerintelligence.reconciliation.web;

import br.com.sellerintelligence.config.TestSecurityConfig;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserRepository;
import br.com.sellerintelligence.reconciliation.domain.*;
import br.com.sellerintelligence.reconciliation.service.SettlementIngestionService;
import br.com.sellerintelligence.sync.domain.DataProvenance;
import br.com.sellerintelligence.sync.domain.Order;
import br.com.sellerintelligence.sync.domain.OrderItem;
import br.com.sellerintelligence.sync.domain.OrderRepository;
import br.com.sellerintelligence.tenancy.domain.Membership;
import br.com.sellerintelligence.tenancy.domain.MembershipRepository;
import br.com.sellerintelligence.tenancy.domain.MembershipStatus;
import br.com.sellerintelligence.tenancy.domain.Organization;
import br.com.sellerintelligence.tenancy.domain.OrganizationRepository;
import br.com.sellerintelligence.tenancy.domain.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class ReconciliationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private ReconciliationSettlementRepository settlementRepository;

    @Autowired
    private ReconciliationSettlementItemRepository itemRepository;

    @Autowired
    private ReconciliationDisputeRepository disputeRepository;

    @Autowired
    private ReconciliationAuditLogRepository auditLogRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Organization org;
    private User user;
    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        disputeRepository.deleteAll();
        auditLogRepository.deleteAll();
        itemRepository.deleteAll();
        settlementRepository.deleteAll();
        orderRepository.deleteAll();
        membershipRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(new User("sub-rec-user", "rec@seller.com", "Reconciliation User"));
        org = organizationRepository.save(new Organization("Reconciliation Org", "rec-org"));
        membershipRepository.save(new Membership(org, user, Role.OWNER, MembershipStatus.ACTIVE));

        sampleOrder = new Order(
                org.getId(), UUID.randomUUID(), br.com.sellerintelligence.tenancy.store.Marketplace.MERCADO_LIVRE,
                "MLB-ORD-100", "PAID",
                new BigDecimal("200.00"), "BRL", Instant.now(), "comprador_teste",
                "SHIP-100", new BigDecimal("15.00"), new BigDecimal("25.00"),
                DataProvenance.IMPORTED, "{}", Instant.now()
        );
        OrderItem orderItem = new OrderItem(org.getId(), sampleOrder, "ITM-1", "SKU-PRO-01", "Item Teste", new BigDecimal("200.00"), 1, BigDecimal.ZERO);
        sampleOrder.addItem(orderItem);
        sampleOrder = orderRepository.save(sampleOrder);
    }

    @Test
    @DisplayName("Should execute complete reconciliation workflow: Import settlement, reconcile item, log dispute, ignore discrepancy and fetch DRE")
    void fullReconciliationWorkflow() throws Exception {
        // 1. Import settlement batch
        SettlementIngestionService.SettlementItemIngestDto item1 = new SettlementIngestionService.SettlementItemIngestDto(
                "MLB-ORD-100", "TX-1001", ReconciliationEntryType.ORDER_PAYOUT, new BigDecimal("160.00")
        );
        SettlementIngestionService.SettlementItemIngestDto item2 = new SettlementIngestionService.SettlementItemIngestDto(
                "MLB-ORD-DISCREPANT", "TX-1002", ReconciliationEntryType.ORDER_PAYOUT, new BigDecimal("80.00")
        );

        SettlementIngestionService.SettlementIngestRequest importReq = new SettlementIngestionService.SettlementIngestRequest(
                "SETTL-BATCH-999", "MERCADO_LIVRE", null, OffsetDateTime.now(),
                new BigDecimal("280.00"), new BigDecimal("240.00"), new BigDecimal("40.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, "extrato_oficial.csv", List.of(item1, item2)
        );

        String settlementJson = mockMvc.perform(post("/api/v1/reconciliation/settlements/import")
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(importReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.externalSettlementId").value("SETTL-BATCH-999"))
                .andExpect(jsonPath("$.grossAmount").value(280.00))
                .andReturn().getResponse().getContentAsString();

        UUID settlementId = UUID.fromString(objectMapper.readTree(settlementJson).get("id").asText());

        // 2. List settlements
        mockMvc.perform(get("/api/v1/reconciliation/settlements")
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(settlementId.toString()));

        // 3. List settlement items
        String itemsJson = mockMvc.perform(get("/api/v1/reconciliation/settlements/" + settlementId + "/items")
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        UUID reconciledItemId = UUID.fromString(objectMapper.readTree(itemsJson).get(0).get("id").asText());
        UUID unidentifiedItemId = UUID.fromString(objectMapper.readTree(itemsJson).get(1).get("id").asText());

        // 4. Manually Associate Order
        AssociateOrderRequest assocReq = new AssociateOrderRequest(sampleOrder.getId(), "Associação manual comprovada com print do marketplace");
        mockMvc.perform(post("/api/v1/reconciliation/items/" + unidentifiedItemId + "/associate")
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assocReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciliationStatus").value("CONCILED"))
                .andExpect(jsonPath("$.matchingMethod").value("MANUAL_ASSOCIATION"));

        // 5. Ignore discrepancy on item
        IgnoreDiscrepancyRequest ignoreReq = new IgnoreDiscrepancyRequest("Abono de centavos aprovado pela diretoria");
        mockMvc.perform(post("/api/v1/reconciliation/items/" + reconciledItemId + "/ignore-discrepancy")
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ignoreReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciliationStatus").value("CONCILED"));

        // 6. Log Dispute Protocol
        LogDisputeRequest disputeReq = new LogDisputeRequest(
                "PROT-ML-554433", "MERCADO_LIVRE", new BigDecimal("15.50"),
                DisputeChannel.HELP_CENTER, "Tarifa de envio cobrada indevidamente em devolução"
        );
        mockMvc.perform(post("/api/v1/reconciliation/items/" + reconciledItemId + "/dispute")
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(disputeReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.protocolNumber").value("PROT-ML-554433"))
                .andExpect(jsonPath("$.disclaimer").isNotEmpty());

        // 7. List Disputes
        mockMvc.perform(get("/api/v1/reconciliation/disputes")
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].protocolNumber").value("PROT-ML-554433"));

        // 8. List Audit Logs for item
        mockMvc.perform(get("/api/v1/reconciliation/items/" + reconciledItemId + "/audit")
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // 9. Fetch Accrual DRE
        mockMvc.perform(get("/api/v1/reconciliation/dre?basis=ACCRUAL")
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basis").value("ACCRUAL"))
                .andExpect(jsonPath("$.lines.length()").value(10))
                .andExpect(jsonPath("$.disclaimer").isNotEmpty());

        // 10. Fetch Cash DRE
        mockMvc.perform(get("/api/v1/reconciliation/dre?basis=CASH")
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basis").value("CASH"))
                .andExpect(jsonPath("$.lines.length()").value(10));

        // 11. Export DRE to CSV
        mockMvc.perform(get("/api/v1/reconciliation/dre/export?basis=ACCRUAL")
                        .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                        .header("X-Tenant-Id", org.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().string(containsString("# RELATÓRIO: DRE GERENCIAL")))
                .andExpect(content().string(containsString("Receita Bruta de Vendas")));
    }
}
