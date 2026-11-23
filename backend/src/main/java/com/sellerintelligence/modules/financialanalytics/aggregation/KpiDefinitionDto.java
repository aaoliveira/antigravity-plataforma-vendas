package com.sellerintelligence.modules.financialanalytics.aggregation;

/**
 * DTO para catálogo e definição formal de um KPI analítico/financeiro.
 */
public record KpiDefinitionDto(
        String code,
        String name,
        String description,
        String formula,
        String unit,
        String provenance
) {}
