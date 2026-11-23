package com.sellerintelligence.modules.financialanalytics.aggregation;

import java.util.Collections;
import java.util.List;

/**
 * Catálogo imutável e oficial de definições dos KPIs da Seller Intelligence Platform.
 * Cumpre a exigência de governança e anti-alucinação: toda fórmula, proveniência e descrição
 * é documentada formalmente e acessível aos operadores e sistemas consumidores.
 */
public final class KpiCatalog {

    private static final List<KpiDefinitionDto> DEFINITIONS = List.of(
            new KpiDefinitionDto(
                    "GROSS_REVENUE",
                    "Receita Bruta",
                    "Soma dos valores brutos dos produtos vendidos nos pedidos faturados do período selecionado.",
                    "SUM(grossRevenue)",
                    "BRL",
                    "OBSERVED / IMPORTED"
            ),
            new KpiDefinitionDto(
                    "DISCOUNT_AMOUNT",
                    "Descontos Concedidos",
                    "Total de abatimentos, cupons e descontos promocionais aplicados aos itens dos pedidos.",
                    "SUM(discountAmount)",
                    "BRL",
                    "OBSERVED / IMPORTED"
            ),
            new KpiDefinitionDto(
                    "NET_REVENUE",
                    "Receita Líquida",
                    "Faturamento líquido efetivo após dedução dos descontos comerciais concedidos.",
                    "GrossRevenue - DiscountAmount",
                    "BRL",
                    "DETERMINISTIC"
            ),
            new KpiDefinitionDto(
                    "CANCELLATION_AMOUNT",
                    "Cancelamentos Brutos",
                    "Montante financeiro de pedidos formalmente cancelados no período.",
                    "SUM(cancellationAmount)",
                    "BRL",
                    "OBSERVED / IMPORTED"
            ),
            new KpiDefinitionDto(
                    "RETURN_AMOUNT",
                    "Devoluções Brutas",
                    "Montante financeiro de itens devolvidos ou reembolsados no período.",
                    "SUM(returnAmount)",
                    "BRL",
                    "OBSERVED / IMPORTED"
            ),
            new KpiDefinitionDto(
                    "MARKETPLACE_FEES",
                    "Comissões de Canal",
                    "Tarifas percentuais cobradas pelos marketplaces (Mercado Livre, Shopee) sobre as vendas faturadas.",
                    "SUM(marketplaceFee)",
                    "BRL",
                    "DETERMINISTIC / IMPORTED"
            ),
            new KpiDefinitionDto(
                    "FIXED_FEES",
                    "Tarifas Fixas por Item",
                    "Tarifas operacionais fixas por transação ou por item cobradas pelos canais de venda.",
                    "SUM(fixedFee)",
                    "BRL",
                    "DETERMINISTIC / IMPORTED"
            ),
            new KpiDefinitionDto(
                    "SHIPPING_COST",
                    "Custo de Envio (Frete)",
                    "Custo logístico de frete cobrado do vendedor pelos marketplaces ou contratos de frete próprio.",
                    "SUM(shippingCost)",
                    "BRL",
                    "OBSERVED / IMPORTED"
            ),
            new KpiDefinitionDto(
                    "PRODUCT_COST",
                    "Custo dos Produtos (CMV)",
                    "Custo das Mercadorias Vendidas apurado com base no cadastro de custo unitário do seller na data do pedido.",
                    "SUM(quantity * unitProductCost)",
                    "BRL",
                    "IMPORTED (Custo Seller)"
            ),
            new KpiDefinitionDto(
                    "PACKAGING_COST",
                    "Custo de Embalagem",
                    "Custos de caixas, envelopes, etiquetas e insumos de expedição cadastrados pelo seller.",
                    "SUM(packagingCost)",
                    "BRL",
                    "IMPORTED (Custo Seller)"
            ),
            new KpiDefinitionDto(
                    "ADDITIONAL_COST",
                    "Custos Adicionais",
                    "Custos operacionais extras atrelados ao manuseio, montagem ou expedição especial.",
                    "SUM(additionalCost)",
                    "BRL",
                    "IMPORTED (Custo Seller)"
            ),
            new KpiDefinitionDto(
                    "TAX_AMOUNT",
                    "Tributos Declarados",
                    "Carga tributária informada pelo seller ou estimada conforme regime fiscal cadastrado (Simples/Lucro Presumido).",
                    "SUM(taxAmount)",
                    "BRL",
                    "USER_INPUT / ESTIMATED"
            ),
            new KpiDefinitionDto(
                    "OTHER_EXPENSES",
                    "Outras Despesas",
                    "Despesas operacionais diversas atreladas diretamente aos pedidos faturados.",
                    "SUM(otherExpensesAmount)",
                    "BRL",
                    "USER_INPUT"
            ),
            new KpiDefinitionDto(
                    "TOTAL_DEDUCTIONS",
                    "Total de Deduções",
                    "Consolidação de todos os custos variáveis diretos, taxas, fretes e tributos incidentes sobre os pedidos.",
                    "SUM(marketplaceFee + fixedFee + shippingCost + productCost + packagingCost + additionalCost + taxAmount + otherExpensesAmount)",
                    "BRL",
                    "DETERMINISTIC"
            ),
            new KpiDefinitionDto(
                    "CONTRIBUTION_MARGIN",
                    "Margem de Contribuição",
                    "Sobra financeira gerada pelas vendas após cobertura de todos os custos e despesas variáveis diretas.",
                    "NetRevenue - TotalDeductions",
                    "BRL",
                    "DETERMINISTIC"
            ),
            new KpiDefinitionDto(
                    "CONTRIBUTION_MARGIN_RATIO",
                    "Margem de Contribuição %",
                    "Eficiência percentual de retenção de margem de contribuição sobre a receita líquida faturada.",
                    "(ContributionMargin / NetRevenue) * 100",
                    "%",
                    "DETERMINISTIC"
            ),
            new KpiDefinitionDto(
                    "AVERAGE_ORDER_VALUE",
                    "Ticket Médio",
                    "Valor médio bruto faturado por pedido concluído no período selecionado.",
                    "GrossRevenue / TotalOrders",
                    "BRL",
                    "DETERMINISTIC"
            ),
            new KpiDefinitionDto(
                    "RECONCILIATION_RATE",
                    "Taxa de Conciliação",
                    "Percentual de pedidos que possuem liquidação externa confirmada via extrato oficial do marketplace.",
                    "(ReconciledOrders / TotalOrders) * 100",
                    "%",
                    "OBSERVED"
            )
    );

    private KpiCatalog() {
    }

    public static List<KpiDefinitionDto> getAllDefinitions() {
        return Collections.unmodifiableList(DEFINITIONS);
    }
}
