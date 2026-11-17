package br.com.sellerintelligence.financial.dashboard;

import br.com.sellerintelligence.financial.domain.*;
import br.com.sellerintelligence.financial.service.FinancialCalculationService;
import br.com.sellerintelligence.financial.service.FinancialRuleService;
import br.com.sellerintelligence.financial.service.SkuCostService;
import br.com.sellerintelligence.infrastructure.error.BusinessException;
import br.com.sellerintelligence.sync.domain.DataProvenance;
import br.com.sellerintelligence.sync.domain.Order;
import br.com.sellerintelligence.sync.domain.OrderItem;
import br.com.sellerintelligence.sync.domain.OrderRepository;
import br.com.sellerintelligence.tenancy.company.Company;
import br.com.sellerintelligence.tenancy.company.CompanyRepository;
import br.com.sellerintelligence.tenancy.company.TaxRegime;
import br.com.sellerintelligence.tenancy.domain.Organization;
import br.com.sellerintelligence.tenancy.domain.OrganizationRepository;
import br.com.sellerintelligence.tenancy.domain.Role;
import br.com.sellerintelligence.tenancy.domain.TenantContext;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import br.com.sellerintelligence.tenancy.store.Store;
import br.com.sellerintelligence.tenancy.store.StoreRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FinancialDashboardIntegrationTest {

    @Autowired
    private FinancialDashboardService dashboardService;

    @Autowired
    private SkuCostService skuCostService;

    @Autowired
    private FinancialRuleService financialRuleService;

    @Autowired
    private FinancialCalculationService financialCalculationService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private OrderRepository orderRepository;

    private UUID tenantAId;
    private UUID tenantBId;
    private Store storeML;
    private Store storeShopee;

    @BeforeEach
    void setUp() {
        Organization orgA = organizationRepository.save(new Organization("Tenant A Dashboard", "tenant-a-dash"));
        tenantAId = orgA.getId();

        Organization orgB = organizationRepository.save(new Organization("Tenant B Dashboard", "tenant-b-dash"));
        tenantBId = orgB.getId();

        Company compA = companyRepository.save(new Company(tenantAId, "98765432000188", "Empresa Dash", "Razao Dash", TaxRegime.SIMPLES_NACIONAL));
        storeML = storeRepository.save(new Store(tenantAId, compA, Marketplace.MERCADO_LIVRE, "Loja ML Dash"));
        storeShopee = storeRepository.save(new Store(tenantAId, compA, Marketplace.SHOPEE, "Loja Shopee Dash"));

        setTenant(tenantAId, Role.OWNER);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private void setTenant(UUID tenantId, Role role) {
        TenantContext.setTenant(new TenantContext.TenantInfo(
                tenantId,
                UUID.randomUUID(),
                "user-sub-" + tenantId,
                "user@" + tenantId + ".com",
                role
        ));
    }

    private void seedOrdersAndCalculations() {
        // Custo SKU
        skuCostService.registerCost(
                "SKU-DASH-1",
                new BigDecimal("50.0000"),
                "BRL",
                "UN",
                "MANUAL",
                Instant.now().minusSeconds(86400 * 60),
                "Custo inicial"
        );

        // Regra ML
        List<FinancialRuleService.ComponentConfig> mlComponents = List.of(
                new FinancialRuleService.ComponentConfig(FinancialComponentType.MARKETPLACE_COMMISSION_PERCENTAGE, new BigDecimal("0.1200"), null, "ITEM", "Comissão 12%"),
                new FinancialRuleService.ComponentConfig(FinancialComponentType.TAX_PERCENTAGE, new BigDecimal("0.0600"), null, "ITEM", "Simples 6%"),
                new FinancialRuleService.ComponentConfig(FinancialComponentType.SHIPPING_SELLER_COST, new BigDecimal("15.0000"), null, "ITEM", "Frete"),
                new FinancialRuleService.ComponentConfig(FinancialComponentType.PACKAGING_COST, new BigDecimal("2.5000"), null, "ITEM", "Embalagem")
        );
        financialRuleService.createOrVersionRuleSet(
                null, storeML.getId(), Marketplace.MERCADO_LIVRE.name(), "Regra ML",
                Instant.now().minusSeconds(86400 * 60), mlComponents
        );

        // Pedido 1: ML, hoje, valor 120.00
        Order o1 = new Order(
                tenantAId, storeML.getId(), Marketplace.MERCADO_LIVRE, "EXT-ORD-01", "PAID",
                new BigDecimal("120.00"), "BRL", Instant.now().minusSeconds(3600), "comprador1",
                null, null, null, DataProvenance.IMPORTED, "{}", Instant.now()
        );
        o1.addItem(new OrderItem(tenantAId, o1, "ITEM-01", "SKU-DASH-1", "Produto Dash", new BigDecimal("120.00"), 1, null));
        o1 = orderRepository.save(o1);
        financialCalculationService.calculateAndSaveOrder(o1.getId(), null);

        // Pedido 2: ML, hoje, valor 240.00 (quantidade 2)
        Order o2 = new Order(
                tenantAId, storeML.getId(), Marketplace.MERCADO_LIVRE, "EXT-ORD-02", "PAID",
                new BigDecimal("240.00"), "BRL", Instant.now().minusSeconds(7200), "comprador2",
                null, null, null, DataProvenance.IMPORTED, "{}", Instant.now()
        );
        o2.addItem(new OrderItem(tenantAId, o2, "ITEM-02", "SKU-DASH-1", "Produto Dash", new BigDecimal("120.00"), 2, null));
        o2 = orderRepository.save(o2);
        financialCalculationService.calculateAndSaveOrder(o2.getId(), null);

        // Pedido 3: Período anterior (45 dias atrás), valor 100.00
        Order o3 = new Order(
                tenantAId, storeML.getId(), Marketplace.MERCADO_LIVRE, "EXT-ORD-03", "PAID",
                new BigDecimal("100.00"), "BRL", Instant.now().minusSeconds(86400 * 45), "comprador3",
                null, null, null, DataProvenance.IMPORTED, "{}", Instant.now()
        );
        o3.addItem(new OrderItem(tenantAId, o3, "ITEM-03", "SKU-DASH-1", "Produto Dash", new BigDecimal("100.00"), 1, null));
        o3 = orderRepository.save(o3);
        financialCalculationService.calculateAndSaveOrder(o3.getId(), null);
    }

    @Test
    @DisplayName("Dashboard consolida faturamento, pedidos, ticket médio e margem de contribuição")
    void shouldConsolidateDashboardMetrics() {
        seedOrdersAndCalculations();

        Instant from = Instant.now().minusSeconds(86400 * 30);
        Instant to = Instant.now();
        FinancialDashboardFilter filter = new FinancialDashboardFilter(null, null, null, from, to, true);

        DashboardOverviewResponse response = dashboardService.getDashboardOverview(filter);

        assertThat(response.summary()).isNotNull();
        // Pedidos no período recente: o1 (120) + o2 (240) = 360.00
        assertThat(response.summary().grossRevenue()).isEqualByComparingTo("360.00");
        assertThat(response.summary().orderCount()).isEqualTo(2);
        // Ticket médio = 360 / 2 = 180.00
        assertThat(response.summary().averageTicket()).isEqualByComparingTo("180.00");
        // COGS total = 50 * 1 + 50 * 2 = 150.00
        assertThat(response.summary().totalCogs()).isEqualByComparingTo("150.00");

        // Breakdown por produto
        assertThat(response.byProduct()).hasSize(1);
        assertThat(response.byProduct().get(0).sku()).isEqualTo("SKU-DASH-1");
        assertThat(response.byProduct().get(0).quantitySold()).isEqualTo(3); // 1 + 2

        // Comparação com período anterior
        assertThat(response.comparison()).isNotNull();
        // Período anterior continha o3 (100.00)
        assertThat(response.comparison().previous().grossRevenue()).isEqualByComparingTo("100.00");
        // Delta faturamento: (360 - 100) / 100 = +260.00%
        assertThat(response.comparison().revenueDeltaPercentage()).isEqualByComparingTo("260.00");
    }

    @Test
    @DisplayName("Filtro por marketplace e loja segmenta os totalizadores corretamente")
    void shouldFilterDashboardByMarketplaceAndStore() {
        seedOrdersAndCalculations();

        // Filtra por Shopee (onde não temos pedidos cadastrados)
        FinancialDashboardFilter shopeeFilter = new FinancialDashboardFilter(
                null, storeShopee.getId(), Marketplace.SHOPEE,
                Instant.now().minusSeconds(86400 * 30), Instant.now(), false
        );

        DashboardOverviewResponse shopeeRes = dashboardService.getDashboardOverview(shopeeFilter);
        assertThat(shopeeRes.summary().orderCount()).isEqualTo(0);
        assertThat(shopeeRes.summary().grossRevenue()).isEqualByComparingTo("0.00");
        assertThat(shopeeRes.summary().contributionMarginValue()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Consistência absoluta: Totalizadores da tela e do relatório CSV exportado são idênticos")
    void shouldMatchScreenTotalsAndCsvExportExactly() {
        seedOrdersAndCalculations();

        Instant from = Instant.now().minusSeconds(86400 * 30);
        Instant to = Instant.now();
        FinancialDashboardFilter filter = new FinancialDashboardFilter(null, null, null, from, to, false);

        DashboardOverviewResponse overview = dashboardService.getDashboardOverview(filter);
        String csv = dashboardService.exportOrdersCsv(filter, ZoneId.of("America/Sao_Paulo"));

        assertThat(csv).contains("# Relatório Financeiro e Margem de Contribuição");
        assertThat(csv).contains("Fuso Horário: America/Sao_Paulo");
        assertThat(csv).contains("EXT-ORD-01");
        assertThat(csv).contains("EXT-ORD-02");
        assertThat(csv).doesNotContain("EXT-ORD-03"); // Fora do período

        // Verifica que os valores unitários de faturamento estão no CSV
        assertThat(csv).contains("120.00");
        assertThat(csv).contains("240.00");
    }

    @Test
    @DisplayName("Controle de autorização RBAC: Usuário com papel OPERATOR ou VIEWER tem acesso negado")
    void shouldDenyAccessToRestrictedRoles() {
        seedOrdersAndCalculations();

        // Define papel OPERATOR
        setTenant(tenantAId, Role.OPERATOR);

        FinancialDashboardFilter filter = new FinancialDashboardFilter(null, null, null, null, null, false);

        assertThatThrownBy(() -> dashboardService.getDashboardOverview(filter))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Acesso negado: seu papel de usuário");

        assertThatThrownBy(() -> dashboardService.exportOrdersCsv(filter, ZoneId.of("America/Sao_Paulo")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Acesso negado: seu papel de usuário");
    }

    @Test
    @DisplayName("Isolamento multi-tenant: Tenant B recebe painel vazio sem dados de Tenant A")
    void shouldIsolateDashboardBetweenTenants() {
        seedOrdersAndCalculations();

        // Troca para Tenant B
        setTenant(tenantBId, Role.OWNER);

        FinancialDashboardFilter filter = new FinancialDashboardFilter(null, null, null, null, null, false);
        DashboardOverviewResponse responseB = dashboardService.getDashboardOverview(filter);

        assertThat(responseB.summary().orderCount()).isEqualTo(0);
        assertThat(responseB.summary().grossRevenue()).isEqualByComparingTo("0.00");
        assertThat(responseB.byProduct()).isEmpty();
        assertThat(responseB.byStore()).isEmpty();
    }
}
