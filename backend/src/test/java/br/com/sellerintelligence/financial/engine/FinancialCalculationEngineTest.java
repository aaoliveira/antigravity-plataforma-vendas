package br.com.sellerintelligence.financial.engine;

import br.com.sellerintelligence.financial.domain.CalculationQuality;
import br.com.sellerintelligence.financial.domain.CalculationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialCalculationEngineTest {

    private FinancialCalculationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new FinancialCalculationEngine();
    }

    @Test
    @DisplayName("Caso 1: Venda Mercado Livre acima de R$ 79 com todas as deduções conhecidas")
    void shouldCalculateAccurateMarginAboveThreshold() {
        // Preço: 120.00, COGS: 50.00, Comissao: 12%, Tarifa Fixa: 6.00 (isento >= 79), Frete: 18.50, Emb: 2.50, Imposto: 6%
        ItemCalculationInput input = new ItemCalculationInput(
                UUID.randomUUID(),
                "SKU-TEST-01",
                new BigDecimal("120.00"),
                1,
                new BigDecimal("50.00"),
                new BigDecimal("0.1200"),
                new BigDecimal("6.00"),
                new BigDecimal("79.00"),
                new BigDecimal("18.50"),
                new BigDecimal("2.50"),
                new BigDecimal("0.0600"),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        ItemCalculationResult result = engine.calculateItem(input);

        assertThat(result.totalPrice()).isEqualByComparingTo("120.00");
        assertThat(result.totalCogs()).isEqualByComparingTo("50.00");
        assertThat(result.marketplaceFee()).isEqualByComparingTo("14.40"); // 120 * 0.12 = 14.40, fixed fee = 0
        assertThat(result.shippingSeller()).isEqualByComparingTo("18.50");
        assertThat(result.packagingCost()).isEqualByComparingTo("2.50");
        assertThat(result.taxValue()).isEqualByComparingTo("7.20"); // 120 * 0.06 = 7.20
        // Total deduções: 50 + 14.40 + 18.50 + 2.50 + 7.20 = 92.60
        // Margem: 120.00 - 92.60 = 27.40
        assertThat(result.contributionMarginValue()).isEqualByComparingTo("27.40");
        // % = 27.40 / 120.00 = 22.8333%
        assertThat(result.contributionMarginPercentage()).isEqualByComparingTo("22.8333");
        assertThat(result.quality()).isEqualTo(CalculationQuality.COMPLETE);
        assertThat(result.memory().missingFields()).isEmpty();
    }

    @Test
    @DisplayName("Caso 2: Venda Mercado Livre abaixo de R$ 79 com incidência de tarifa fixa de R$ 6,00")
    void shouldCalculateAccurateMarginBelowThresholdWithFixedFee() {
        // Preço: 45.00, COGS: 15.00, Comissao: 14%, Tarifa Fixa: 6.00, Frete: 0.00, Emb: 1.20, Imposto: 4%
        ItemCalculationInput input = new ItemCalculationInput(
                UUID.randomUUID(),
                "SKU-TEST-02",
                new BigDecimal("45.00"),
                1,
                new BigDecimal("15.00"),
                new BigDecimal("0.1400"),
                new BigDecimal("6.00"),
                new BigDecimal("79.00"),
                BigDecimal.ZERO,
                new BigDecimal("1.20"),
                new BigDecimal("0.0400"),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        ItemCalculationResult result = engine.calculateItem(input);

        assertThat(result.totalPrice()).isEqualByComparingTo("45.00");
        assertThat(result.totalCogs()).isEqualByComparingTo("15.00");
        // Comissao: 45 * 0.14 = 6.30 + Tarifa fixa: 6.00 = 12.30
        assertThat(result.marketplaceFee()).isEqualByComparingTo("12.30");
        assertThat(result.shippingSeller()).isEqualByComparingTo("0.00");
        assertThat(result.packagingCost()).isEqualByComparingTo("1.20");
        assertThat(result.taxValue()).isEqualByComparingTo("1.80"); // 45 * 0.04 = 1.80
        // Total deduções: 15.00 + 12.30 + 1.20 + 1.80 = 30.30
        // Margem: 45.00 - 30.30 = 14.70
        assertThat(result.contributionMarginValue()).isEqualByComparingTo("14.70");
        // % = 14.70 / 45.00 = 32.6667%
        assertThat(result.contributionMarginPercentage()).isEqualByComparingTo("32.6667");
        assertThat(result.quality()).isEqualTo(CalculationQuality.COMPLETE);
    }

    @Test
    @DisplayName("Caso 3: Cálculo parcial com ausência de custo COGS e alíquota de imposto")
    void shouldFlagCalculationAsPartialWhenDataIsMissing() {
        ItemCalculationInput input = new ItemCalculationInput(
                UUID.randomUUID(),
                "SKU-UNKNOWN",
                new BigDecimal("100.00"),
                1,
                null, // COGS ausente
                new BigDecimal("0.1600"),
                BigDecimal.ZERO,
                new BigDecimal("79.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null, // Imposto ausente
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        ItemCalculationResult result = engine.calculateItem(input);

        assertThat(result.quality()).isEqualTo(CalculationQuality.PARTIAL);
        assertThat(result.memory().missingFields()).containsExactlyInAnyOrder("COGS", "TAX");
        assertThat(result.totalCogs()).isEqualByComparingTo("0.00");
        assertThat(result.taxValue()).isEqualByComparingTo("0.00");
        assertThat(result.memory().assumptions()).contains("Cálculo gerencial preliminar");
    }

    @Test
    @DisplayName("Caso 4: Simulação de preço mínimo (break-even) e preço alvo")
    void shouldSimulateMinimumAndTargetPrice() {
        PriceSimulationInput input = new PriceSimulationInput(
                new BigDecimal("50.00"), // COGS
                new BigDecimal("2.50"),  // Emb
                new BigDecimal("18.50"), // Frete
                new BigDecimal("6.00"),  // Tarifa fixa
                new BigDecimal("79.00"), // Limiar
                new BigDecimal("0.1200"),// Comissao 12%
                new BigDecimal("0.0600"),// Imposto 6%
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("20.00"), // Margem alvo de 20%
                null
        );

        PriceSimulationResult result = engine.simulatePrice(input, null);

        // Deduções percentuais = 12% + 6% = 18% (0.18)
        // Custos fixos acima de R$ 79: 50.00 + 2.50 + 18.50 = 71.00
        // Break-even: 71.00 / (1 - 0.18) = 71.00 / 0.82 = 86.59 (já que 86.59 >= 79, sem tarifa fixa)
        assertThat(result.minimumPrice()).isEqualByComparingTo("86.59");

        // Alvo (20% margem): Denominador = 1 - 0.18 - 0.20 = 0.62
        // Preço alvo = 71.00 / 0.62 = 114.52
        assertThat(result.targetPrice()).isEqualByComparingTo("114.52");
        assertThat(result.quality()).isEqualTo(CalculationQuality.SIMULATED);
    }

    @Test
    @DisplayName("Caso 5: Cancelamento de pedido reverte receita e despesas com margem zero")
    void shouldHandleCancelledOrderProperly() {
        OrderCalculationInput input = new OrderCalculationInput(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                CalculationStatus.CANCELLED,
                List.of(),
                new BigDecimal("25.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        OrderCalculationResult result = engine.calculateOrder(input);

        assertThat(result.status()).isEqualTo(CalculationStatus.CANCELLED);
        assertThat(result.grossRevenue()).isEqualByComparingTo("0.00");
        assertThat(result.netRevenue()).isEqualByComparingTo("0.00");
        assertThat(result.contributionMarginValue()).isEqualByComparingTo("0.00");
    }
}
