package com.sellerintelligence.modules.listinganalytics;

import com.sellerintelligence.modules.listinganalytics.model.MetricAvailabilityStatus;
import com.sellerintelligence.modules.listinganalytics.model.MetricCalculationResult;
import com.sellerintelligence.modules.listinganalytics.model.MetricFormulaEngine;
import com.sellerintelligence.modules.listinganalytics.model.MetricProvenance;
import com.sellerintelligence.modules.listinganalytics.model.MetricUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MetricFormulaEngineTest {

    private MetricFormulaEngine formulaEngine;

    @BeforeEach
    void setUp() {
        formulaEngine = new MetricFormulaEngine();
    }

    // =========================================================================
    // CTR Tests: clicks / impressions * 100
    // =========================================================================

    @Test
    @DisplayName("CTR: Deve calcular corretamente CTR quando cliques e impressões forem válidos")
    void shouldCalculateCtrCorrectly() {
        BigDecimal clicks = new BigDecimal("45");
        BigDecimal impressions = new BigDecimal("1500");

        MetricCalculationResult result = formulaEngine.calculateCtr(clicks, impressions);

        assertThat(result.metricKey()).isEqualTo("CTR");
        assertThat(result.status()).isEqualTo(MetricAvailabilityStatus.AVAILABLE);
        assertThat(result.provenance()).isEqualTo(MetricProvenance.DERIVED_FORMULA);
        assertThat(result.unit()).isEqualTo(MetricUnit.PERCENTAGE);
        // 45 / 1500 = 0.030000 -> 3.0000%
        assertThat(result.value()).isEqualByComparingTo("3.0000");
        assertThat(result.unavailabilityReason()).isNull();
        assertThat(result.numeratorMetric()).isEqualTo("CLICKS");
        assertThat(result.numeratorValue()).isEqualByComparingTo("45");
        assertThat(result.denominatorMetric()).isEqualTo("IMPRESSIONS");
        assertThat(result.denominatorValue()).isEqualByComparingTo("1500");
    }

    @Test
    @DisplayName("CTR: Deve retornar UNAVAILABLE com valor nulo se impressões ou cliques forem nulos (Anti-Alucinação)")
    void shouldReturnUnavailableWhenImpressionsOrClicksAreNull() {
        MetricCalculationResult result1 = formulaEngine.calculateCtr(new BigDecimal("10"), null);
        assertThat(result1.status()).isEqualTo(MetricAvailabilityStatus.UNAVAILABLE);
        assertThat(result1.value()).isNull();
        assertThat(result1.unavailabilityReason()).contains("Impressões ou cliques ausentes");

        MetricCalculationResult result2 = formulaEngine.calculateCtr(null, new BigDecimal("1000"));
        assertThat(result2.status()).isEqualTo(MetricAvailabilityStatus.UNAVAILABLE);
        assertThat(result2.value()).isNull();
    }

    @Test
    @DisplayName("CTR: Deve tratar divisão por zero retornando INSUFFICIENT_DATA com valor 0.0000 e denominador registrado")
    void shouldHandleDivisionByZeroInCtr() {
        BigDecimal clicks = BigDecimal.ZERO;
        BigDecimal impressions = BigDecimal.ZERO;

        MetricCalculationResult result = formulaEngine.calculateCtr(clicks, impressions);

        assertThat(result.status()).isEqualTo(MetricAvailabilityStatus.INSUFFICIENT_DATA);
        assertThat(result.value()).isEqualByComparingTo("0.0000");
        assertThat(result.unavailabilityReason()).contains("Denominador de impressões igual a zero");
        assertThat(result.denominatorValue()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("CTR: Deve rejeitar anomalia estatística quando cliques excederem impressões")
    void shouldRejectWhenClicksExceedImpressions() {
        BigDecimal clicks = new BigDecimal("150");
        BigDecimal impressions = new BigDecimal("100");

        MetricCalculationResult result = formulaEngine.calculateCtr(clicks, impressions);

        assertThat(result.status()).isEqualTo(MetricAvailabilityStatus.UNAVAILABLE);
        assertThat(result.value()).isNull();
        assertThat(result.unavailabilityReason()).contains("Inconsistência estatística: número de cliques (150) não pode exceder o número de impressões (100)");
    }

    @Test
    @DisplayName("CTR: Deve rejeitar valores negativos")
    void shouldRejectNegativeValuesInCtr() {
        MetricCalculationResult result = formulaEngine.calculateCtr(new BigDecimal("-5"), new BigDecimal("100"));
        assertThat(result.status()).isEqualTo(MetricAvailabilityStatus.UNAVAILABLE);
        assertThat(result.value()).isNull();
        assertThat(result.unavailabilityReason()).contains("Valores negativos");
    }

    // =========================================================================
    // Conversão por Pedidos: orders_count / visits * 100
    // =========================================================================

    @Test
    @DisplayName("Conversão Pedidos: Deve calcular corretamente taxa de conversão")
    void shouldCalculateConversionRateOrdersCorrectly() {
        BigDecimal orders = new BigDecimal("25");
        BigDecimal visits = new BigDecimal("1000");

        MetricCalculationResult result = formulaEngine.calculateConversionRateOrders(orders, visits);

        assertThat(result.status()).isEqualTo(MetricAvailabilityStatus.AVAILABLE);
        assertThat(result.provenance()).isEqualTo(MetricProvenance.DERIVED_FORMULA);
        // 25 / 1000 = 0.025000 -> 2.5000%
        assertThat(result.value()).isEqualByComparingTo("2.5000");
        assertThat(result.numeratorMetric()).isEqualTo("ORDERS_COUNT");
        assertThat(result.denominatorMetric()).isEqualTo("VISITS");
    }

    @Test
    @DisplayName("Conversão Pedidos: Deve retornar UNAVAILABLE se visitas forem nulas")
    void shouldReturnUnavailableWhenVisitsAreNull() {
        MetricCalculationResult result = formulaEngine.calculateConversionRateOrders(new BigDecimal("10"), null);
        assertThat(result.status()).isEqualTo(MetricAvailabilityStatus.UNAVAILABLE);
        assertThat(result.value()).isNull();
        assertThat(result.unavailabilityReason()).contains("Visitas não fornecidas");
    }

    @Test
    @DisplayName("Conversão Pedidos: Deve retornar INSUFFICIENT_DATA se visitas forem zero (sem divisão por zero)")
    void shouldHandleZeroVisitsInConversionRateOrders() {
        MetricCalculationResult result = formulaEngine.calculateConversionRateOrders(BigDecimal.ZERO, BigDecimal.ZERO);
        assertThat(result.status()).isEqualTo(MetricAvailabilityStatus.INSUFFICIENT_DATA);
        assertThat(result.value()).isEqualByComparingTo("0.0000");
        assertThat(result.unavailabilityReason()).contains("Denominador de visitas igual a zero");
    }

    @Test
    @DisplayName("Conversão Pedidos: 0 pedidos com 500 visitas resulta em 0.0000% AVAILABLE")
    void shouldReturnZeroPercentWhenZeroOrdersAndPositiveVisits() {
        MetricCalculationResult result = formulaEngine.calculateConversionRateOrders(BigDecimal.ZERO, new BigDecimal("500"));
        assertThat(result.status()).isEqualTo(MetricAvailabilityStatus.AVAILABLE);
        assertThat(result.value()).isEqualByComparingTo("0.0000");
    }

    // =========================================================================
    // Conversão por Unidades: units_sold / visits * 100
    // =========================================================================

    @Test
    @DisplayName("Conversão Unidades: Deve calcular taxa de unidades por visita")
    void shouldCalculateConversionRateUnitsCorrectly() {
        BigDecimal units = new BigDecimal("50");
        BigDecimal visits = new BigDecimal("1000");

        MetricCalculationResult result = formulaEngine.calculateConversionRateUnits(units, visits);

        assertThat(result.status()).isEqualTo(MetricAvailabilityStatus.AVAILABLE);
        // 50 / 1000 = 5.0000%
        assertThat(result.value()).isEqualByComparingTo("5.0000");
        assertThat(result.numeratorMetric()).isEqualTo("UNITS_SOLD");
    }

    @Test
    @DisplayName("Conversão Unidades: Deve tratar visitas nulas ou zero de forma determinística")
    void shouldHandleNullOrZeroVisitsInConversionUnits() {
        MetricCalculationResult resNull = formulaEngine.calculateConversionRateUnits(new BigDecimal("10"), null);
        assertThat(resNull.status()).isEqualTo(MetricAvailabilityStatus.UNAVAILABLE);
        assertThat(resNull.value()).isNull();

        MetricCalculationResult resZero = formulaEngine.calculateConversionRateUnits(BigDecimal.ZERO, BigDecimal.ZERO);
        assertThat(resZero.status()).isEqualTo(MetricAvailabilityStatus.INSUFFICIENT_DATA);
        assertThat(resZero.value()).isEqualByComparingTo("0.0000");
    }
}
