package com.sellerintelligence.modules.listinganalytics.model;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Motor determinístico de cálculo de taxas de desempenho (CTR e Conversão).
 * Aplica aritmética de alta precisão em BigDecimal, tratando divisão por zero
 * e rejeitando terminantemente preencher dados ausentes com estimativas silenciosas.
 */
@Component
public class MetricFormulaEngine {

    public static final int RATIO_SCALE = 6;
    public static final int PERCENTAGE_SCALE = 4;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    /**
     * Calcula o CTR: clicks / impressions * 100
     */
    public MetricCalculationResult calculateCtr(BigDecimal clicks, BigDecimal impressions) {
        String formula = "clicks / impressions * 100";
        String numMetric = "CLICKS";
        String denMetric = "IMPRESSIONS";

        if (impressions == null || clicks == null) {
            return new MetricCalculationResult(
                    "CTR",
                    null,
                    MetricUnit.PERCENTAGE,
                    MetricProvenance.UNAVAILABLE,
                    MetricAvailabilityStatus.UNAVAILABLE,
                    "Impressões ou cliques ausentes para o cálculo da taxa de cliques (CTR).",
                    formula,
                    numMetric,
                    clicks,
                    denMetric,
                    impressions
            );
        }

        if (clicks.compareTo(BigDecimal.ZERO) < 0 || impressions.compareTo(BigDecimal.ZERO) < 0) {
            return new MetricCalculationResult(
                    "CTR",
                    null,
                    MetricUnit.PERCENTAGE,
                    MetricProvenance.UNAVAILABLE,
                    MetricAvailabilityStatus.UNAVAILABLE,
                    "Valores negativos de cliques ou impressões são inválidos.",
                    formula,
                    numMetric,
                    clicks,
                    denMetric,
                    impressions
            );
        }

        if (impressions.compareTo(BigDecimal.ZERO) == 0) {
            return new MetricCalculationResult(
                    "CTR",
                    BigDecimal.ZERO.setScale(PERCENTAGE_SCALE, ROUNDING),
                    MetricUnit.PERCENTAGE,
                    MetricProvenance.DERIVED_FORMULA,
                    MetricAvailabilityStatus.INSUFFICIENT_DATA,
                    "Denominador de impressões igual a zero no período avaliado.",
                    formula,
                    numMetric,
                    clicks,
                    denMetric,
                    impressions
            );
        }

        if (clicks.compareTo(impressions) > 0) {
            return new MetricCalculationResult(
                    "CTR",
                    null,
                    MetricUnit.PERCENTAGE,
                    MetricProvenance.UNAVAILABLE,
                    MetricAvailabilityStatus.UNAVAILABLE,
                    "Inconsistência estatística: número de cliques (" + clicks + ") não pode exceder o número de impressões (" + impressions + ").",
                    formula,
                    numMetric,
                    clicks,
                    denMetric,
                    impressions
            );
        }

        BigDecimal ratio = clicks.divide(impressions, RATIO_SCALE, ROUNDING);
        BigDecimal percentage = ratio.multiply(BigDecimal.valueOf(100)).setScale(PERCENTAGE_SCALE, ROUNDING);

        return new MetricCalculationResult(
                "CTR",
                percentage,
                MetricUnit.PERCENTAGE,
                MetricProvenance.DERIVED_FORMULA,
                MetricAvailabilityStatus.AVAILABLE,
                null,
                formula,
                numMetric,
                clicks,
                denMetric,
                impressions
        );
    }

    /**
     * Calcula Taxa de Conversão por Pedidos: orders_count / visits * 100
     */
    public MetricCalculationResult calculateConversionRateOrders(BigDecimal ordersCount, BigDecimal visits) {
        String formula = "orders_count / visits * 100";
        String numMetric = "ORDERS_COUNT";
        String denMetric = "VISITS";

        if (visits == null || ordersCount == null) {
            return new MetricCalculationResult(
                    "CONVERSION_RATE_ORDERS",
                    null,
                    MetricUnit.PERCENTAGE,
                    MetricProvenance.UNAVAILABLE,
                    MetricAvailabilityStatus.UNAVAILABLE,
                    "Visitas não fornecidas pelo canal para cálculo da taxa de conversão por pedidos.",
                    formula,
                    numMetric,
                    ordersCount,
                    denMetric,
                    visits
            );
        }

        if (ordersCount.compareTo(BigDecimal.ZERO) < 0 || visits.compareTo(BigDecimal.ZERO) < 0) {
            return new MetricCalculationResult(
                    "CONVERSION_RATE_ORDERS",
                    null,
                    MetricUnit.PERCENTAGE,
                    MetricProvenance.UNAVAILABLE,
                    MetricAvailabilityStatus.UNAVAILABLE,
                    "Valores negativos de pedidos ou visitas são inválidos.",
                    formula,
                    numMetric,
                    ordersCount,
                    denMetric,
                    visits
            );
        }

        if (visits.compareTo(BigDecimal.ZERO) == 0) {
            return new MetricCalculationResult(
                    "CONVERSION_RATE_ORDERS",
                    BigDecimal.ZERO.setScale(PERCENTAGE_SCALE, ROUNDING),
                    MetricUnit.PERCENTAGE,
                    MetricProvenance.DERIVED_FORMULA,
                    MetricAvailabilityStatus.INSUFFICIENT_DATA,
                    "Denominador de visitas igual a zero no período avaliado.",
                    formula,
                    numMetric,
                    ordersCount,
                    denMetric,
                    visits
            );
        }

        BigDecimal ratio = ordersCount.divide(visits, RATIO_SCALE, ROUNDING);
        BigDecimal percentage = ratio.multiply(BigDecimal.valueOf(100)).setScale(PERCENTAGE_SCALE, ROUNDING);

        return new MetricCalculationResult(
                "CONVERSION_RATE_ORDERS",
                percentage,
                MetricUnit.PERCENTAGE,
                MetricProvenance.DERIVED_FORMULA,
                MetricAvailabilityStatus.AVAILABLE,
                null,
                formula,
                numMetric,
                ordersCount,
                denMetric,
                visits
        );
    }

    /**
     * Calcula Taxa de Conversão por Unidades: units_sold / visits * 100
     */
    public MetricCalculationResult calculateConversionRateUnits(BigDecimal unitsSold, BigDecimal visits) {
        String formula = "units_sold / visits * 100";
        String numMetric = "UNITS_SOLD";
        String denMetric = "VISITS";

        if (visits == null || unitsSold == null) {
            return new MetricCalculationResult(
                    "CONVERSION_RATE_UNITS",
                    null,
                    MetricUnit.PERCENTAGE,
                    MetricProvenance.UNAVAILABLE,
                    MetricAvailabilityStatus.UNAVAILABLE,
                    "Visitas não fornecidas pelo canal para cálculo da taxa de conversão por unidades.",
                    formula,
                    numMetric,
                    unitsSold,
                    denMetric,
                    visits
            );
        }

        if (unitsSold.compareTo(BigDecimal.ZERO) < 0 || visits.compareTo(BigDecimal.ZERO) < 0) {
            return new MetricCalculationResult(
                    "CONVERSION_RATE_UNITS",
                    null,
                    MetricUnit.PERCENTAGE,
                    MetricProvenance.UNAVAILABLE,
                    MetricAvailabilityStatus.UNAVAILABLE,
                    "Valores negativos de unidades vendidas ou visitas são inválidos.",
                    formula,
                    numMetric,
                    unitsSold,
                    denMetric,
                    visits
            );
        }

        if (visits.compareTo(BigDecimal.ZERO) == 0) {
            return new MetricCalculationResult(
                    "CONVERSION_RATE_UNITS",
                    BigDecimal.ZERO.setScale(PERCENTAGE_SCALE, ROUNDING),
                    MetricUnit.PERCENTAGE,
                    MetricProvenance.DERIVED_FORMULA,
                    MetricAvailabilityStatus.INSUFFICIENT_DATA,
                    "Denominador de visitas igual a zero no período avaliado.",
                    formula,
                    numMetric,
                    unitsSold,
                    denMetric,
                    visits
            );
        }

        BigDecimal ratio = unitsSold.divide(visits, RATIO_SCALE, ROUNDING);
        BigDecimal percentage = ratio.multiply(BigDecimal.valueOf(100)).setScale(PERCENTAGE_SCALE, ROUNDING);

        return new MetricCalculationResult(
                "CONVERSION_RATE_UNITS",
                percentage,
                MetricUnit.PERCENTAGE,
                MetricProvenance.DERIVED_FORMULA,
                MetricAvailabilityStatus.AVAILABLE,
                null,
                formula,
                numMetric,
                unitsSold,
                denMetric,
                visits
        );
    }
}
