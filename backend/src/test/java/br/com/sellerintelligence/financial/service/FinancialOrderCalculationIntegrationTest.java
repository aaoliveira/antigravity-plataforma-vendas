package br.com.sellerintelligence.financial.service;

import br.com.sellerintelligence.financial.domain.*;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FinancialOrderCalculationIntegrationTest {

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
    private Store storeA;

    @BeforeEach
    void setUp() {
        Organization orgA = organizationRepository.save(new Organization("Tenant A Fin", "tenant-a-fin"));
        tenantAId = orgA.getId();

        Organization orgB = organizationRepository.save(new Organization("Tenant B Fin", "tenant-b-fin"));
        tenantBId = orgB.getId();

        Company compA = companyRepository.save(new Company(tenantAId, "12345678000199", "Empresa A", "Razao A", TaxRegime.SIMPLES_NACIONAL));
        storeA = storeRepository.save(new Store(tenantAId, compA, Marketplace.MERCADO_LIVRE, "Loja ML A"));

        setTenant(tenantAId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private void setTenant(UUID tenantId) {
        TenantContext.setTenant(new TenantContext.TenantInfo(
                tenantId,
                UUID.randomUUID(),
                "user-sub-" + tenantId,
                "user@" + tenantId + ".com",
                Role.OWNER
        ));
    }

    @Test
    @DisplayName("Gestão e histórico de custos de SKU com vigência temporal")
    void shouldRegisterSkuCostsWithValidityAndHistory() {
        Instant t0 = Instant.now().minusSeconds(3600);
        SkuCost c1 = skuCostService.registerCost(
                "SKU-ABC",
                new BigDecimal("40.0000"),
                "BRL",
                "UN",
                "MANUAL",
                t0,
                "Custo inicial de lote"
        );
        assertThat(c1.getId()).isNotNull();
        assertThat(c1.getCostPrice()).isEqualByComparingTo("40.0000");

        // Cadastra novo custo hoje
        Instant t1 = Instant.now();
        SkuCost c2 = skuCostService.registerCost(
                "SKU-ABC",
                new BigDecimal("45.5000"),
                "BRL",
                "UN",
                "MANUAL",
                t1,
                "Reajuste de fornecedor"
        );

        // O custo efetivo atual deve ser 45.50
        Optional<SkuCost> effective = skuCostService.getEffectiveCost("SKU-ABC", Instant.now());
        assertThat(effective).isPresent();
        assertThat(effective.get().getCostPrice()).isEqualByComparingTo("45.5000");

        // O histórico deve conter 2 registros ordenados pelo mais recente
        List<SkuCost> history = skuCostService.getCostHistory("SKU-ABC");
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getCostPrice()).isEqualByComparingTo("45.5000");
        assertThat(history.get(1).getCostPrice()).isEqualByComparingTo("40.0000");
        assertThat(history.get(1).getEffectiveTo()).isNotNull();
    }

    @Test
    @DisplayName("Criação e versionamento de regras financeiras com preservação histórica")
    void shouldCreateAndVersionFinancialRuleSets() {
        List<FinancialRuleService.ComponentConfig> componentsV1 = List.of(
                new FinancialRuleService.ComponentConfig(FinancialComponentType.MARKETPLACE_COMMISSION_PERCENTAGE, new BigDecimal("0.1200"), null, "ITEM", "Comissão 12%"),
                new FinancialRuleService.ComponentConfig(FinancialComponentType.TAX_PERCENTAGE, new BigDecimal("0.0600"), null, "ITEM", "Simples 6%"),
                new FinancialRuleService.ComponentConfig(FinancialComponentType.PACKAGING_COST, new BigDecimal("2.5000"), null, "ITEM", "Caixa padrão")
        );

        FinancialRuleSet ruleV1 = financialRuleService.createOrVersionRuleSet(
                null,
                storeA.getId(),
                Marketplace.MERCADO_LIVRE.name(),
                "Regra ML Clássico",
                Instant.now().minusSeconds(7200),
                componentsV1
        );

        assertThat(ruleV1.getVersion()).isEqualTo(1);
        assertThat(ruleV1.isActive()).isTrue();

        // Atualização da regra para nova alíquota gera versão 2
        List<FinancialRuleService.ComponentConfig> componentsV2 = List.of(
                new FinancialRuleService.ComponentConfig(FinancialComponentType.MARKETPLACE_COMMISSION_PERCENTAGE, new BigDecimal("0.1400"), null, "ITEM", "Comissão 14%"),
                new FinancialRuleService.ComponentConfig(FinancialComponentType.TAX_PERCENTAGE, new BigDecimal("0.0600"), null, "ITEM", "Simples 6%"),
                new FinancialRuleService.ComponentConfig(FinancialComponentType.PACKAGING_COST, new BigDecimal("2.5000"), null, "ITEM", "Caixa padrão")
        );

        FinancialRuleSet ruleV2 = financialRuleService.createOrVersionRuleSet(
                null,
                storeA.getId(),
                Marketplace.MERCADO_LIVRE.name(),
                "Regra ML Clássico",
                Instant.now(),
                componentsV2
        );

        assertThat(ruleV2.getVersion()).isEqualTo(2);
        assertThat(ruleV2.isActive()).isTrue();

        // Regra v1 deve estar inativa
        Optional<FinancialRuleSet> v1Reloaded = financialRuleService.getRuleById(ruleV1.getId());
        assertThat(v1Reloaded).isPresent();
        assertThat(v1Reloaded.get().isActive()).isFalse();
        assertThat(v1Reloaded.get().getEffectiveTo()).isNotNull();
    }

    @Test
    @DisplayName("Cálculo financeiro completo de pedido e persistência de memória auditável")
    void shouldCalculateAndPersistOrderCalculationWithMemory() {
        // 1. Cadastra custo de SKU
        skuCostService.registerCost(
                "SKU-NOTEBOOK",
                new BigDecimal("2000.0000"),
                "BRL",
                "UN",
                "MANUAL",
                Instant.now().minusSeconds(3600),
                "Custo Dell G15"
        );

        // 2. Cadastra regra financeira
        List<FinancialRuleService.ComponentConfig> components = List.of(
                new FinancialRuleService.ComponentConfig(FinancialComponentType.MARKETPLACE_COMMISSION_PERCENTAGE, new BigDecimal("0.1000"), null, "ITEM", "Comissão 10%"),
                new FinancialRuleService.ComponentConfig(FinancialComponentType.TAX_PERCENTAGE, new BigDecimal("0.0500"), null, "ITEM", "Simples 5%"),
                new FinancialRuleService.ComponentConfig(FinancialComponentType.SHIPPING_SELLER_COST, new BigDecimal("45.0000"), null, "ITEM", "Frete"),
                new FinancialRuleService.ComponentConfig(FinancialComponentType.PACKAGING_COST, new BigDecimal("10.0000"), null, "ITEM", "Embalagem reforçada")
        );
        financialRuleService.createOrVersionRuleSet(
                null,
                storeA.getId(),
                Marketplace.MERCADO_LIVRE.name(),
                "Regra Eletrônicos",
                Instant.now().minusSeconds(3600),
                components
        );

        // 3. Cadastra pedido no sync
        Order order = new Order(
                tenantAId,
                storeA.getId(),
                Marketplace.MERCADO_LIVRE,
                "ORDER-EXT-9988",
                "PAID",
                new BigDecimal("3000.00"),
                "BRL",
                Instant.now().minusSeconds(1800),
                "comprador_teste",
                "SHIP-9988",
                null,
                null,
                DataProvenance.IMPORTED,
                "{}",
                Instant.now()
        );
        order.addItem(new OrderItem(tenantAId, order, "ITEM-EXT-01", "SKU-NOTEBOOK", "Notebook Dell G15", new BigDecimal("3000.00"), 1, null));
        order = orderRepository.save(order);

        // 4. Executa cálculo financeiro
        OrderFinancialCalculation calc = financialCalculationService.calculateAndSaveOrder(order.getId(), null);

        assertThat(calc.getCalculationQuality()).isEqualTo(CalculationQuality.COMPLETE);
        assertThat(calc.getGrossRevenue()).isEqualByComparingTo("3000.00");
        assertThat(calc.getTotalCogs()).isEqualByComparingTo("2000.00");
        assertThat(calc.getTotalMarketplaceFees()).isEqualByComparingTo("300.00"); // 3000 * 0.10 = 300.00
        assertThat(calc.getTotalTaxes()).isEqualByComparingTo("150.00"); // 3000 * 0.05 = 150.00
        assertThat(calc.getTotalShippingSeller()).isEqualByComparingTo("45.00");
        assertThat(calc.getTotalPackaging()).isEqualByComparingTo("10.00");
        // Deduções: 2000 + 300 + 150 + 45 + 10 = 2505.00
        // Margem: 3000 - 2505 = 495.00
        assertThat(calc.getContributionMarginValue()).isEqualByComparingTo("495.00");
        // %: 495 / 3000 = 16.5000%
        assertThat(calc.getContributionMarginPercentage()).isEqualByComparingTo("16.5000");

        // Verifica item e memória JSON
        assertThat(calc.getItems()).hasSize(1);
        OrderItemFinancialCalculation itemCalc = calc.getItems().get(0);
        assertThat(itemCalc.getMemoryBreakdown()).contains("RECEITA_BRUTA");
        assertThat(itemCalc.getMemoryBreakdown()).contains("MARGEM_CONTRIBUICAO_LIQUIDA");
    }

    @Test
    @DisplayName("Cancelamento e devolução de pedidos revertem valores conforme política contábil")
    void shouldHandleCancellationAndReturnPolicies() {
        Order order = new Order(
                tenantAId,
                storeA.getId(),
                Marketplace.MERCADO_LIVRE,
                "ORDER-EXT-CANCEL-01",
                "PAID",
                new BigDecimal("500.00"),
                "BRL",
                Instant.now(),
                "comprador",
                null,
                null,
                null,
                DataProvenance.IMPORTED,
                "{}",
                Instant.now()
        );
        order.addItem(new OrderItem(tenantAId, order, "ITEM-01", "SKU-GEN", "Item Genérico", new BigDecimal("500.00"), 1, null));
        order = orderRepository.save(order);

        financialCalculationService.calculateAndSaveOrder(order.getId(), null);

        // Trata cancelamento
        OrderFinancialCalculation cancelled = financialCalculationService.handleOrderCancellation(order.getId(), "Cancelado pelo cliente");
        assertThat(cancelled.getCalculationStatus()).isEqualTo(CalculationStatus.CANCELLED);
        assertThat(cancelled.getGrossRevenue()).isEqualByComparingTo("0.00");
        assertThat(cancelled.getContributionMarginValue()).isEqualByComparingTo("0.00");

        // Trata devolução com prejuízo logístico
        OrderFinancialCalculation returned = financialCalculationService.handleOrderReturn(
                order.getId(),
                new BigDecimal("30.00"), // Frete ida/volta
                new BigDecimal("5.00"),  // Embalagem avariada
                BigDecimal.ZERO,         // Produto intacto
                "Devolvido por arrependimento"
        );
        assertThat(returned.getCalculationStatus()).isEqualTo(CalculationStatus.RETURNED);
        assertThat(returned.getContributionMarginValue()).isEqualByComparingTo("-35.00");
    }

    @Test
    @DisplayName("Isolamento multi-tenant estrito: Tenant B não pode consultar nem alterar cálculo de Tenant A")
    void shouldIsolateFinancialCalculationsBetweenTenants() {
        Order orderA = new Order(
                tenantAId,
                storeA.getId(),
                Marketplace.MERCADO_LIVRE,
                "ORDER-EXT-ISOLATED",
                "PAID",
                new BigDecimal("200.00"),
                "BRL",
                Instant.now(),
                "comprador",
                null,
                null,
                null,
                DataProvenance.IMPORTED,
                "{}",
                Instant.now()
        );
        Order savedOrderA = orderRepository.save(orderA);
        final UUID targetOrderId = savedOrderA.getId();
        financialCalculationService.calculateAndSaveOrder(targetOrderId, null);

        // Troca para Tenant B: cálculo de Tenant A deve ser inacessível (empty)
        setTenant(tenantBId);

        assertThat(financialCalculationService.getOrderCalculation(targetOrderId)).isEmpty();

        assertThatThrownBy(() -> financialCalculationService.recalculateHistoricalOrder(targetOrderId, null, "Tentativa não autorizada"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cálculo anterior não encontrado");
    }
}
