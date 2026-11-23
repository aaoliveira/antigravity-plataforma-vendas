# Especificação Técnica: P022-BE - Endpoints Agregados Read-Only, Cache Tenant-Aware e Invalidação por Evento

## 1. Contexto & Objetivos

Esta especificação define o backend do **P022-BE**, implementando os **Endpoints Agregados Read-Only de KPIs Financeiros e Analíticos com Cache Tenant-Aware e Invalidação Reativa por Eventos** na **Seller Intelligence Platform**. 

O objetivo é fornecer ao frontend e aos sistemas consumidores visões consolidadas e sumarizações analíticas de alto desempenho (p95 < 500 ms) para tomadas de decisão rápidas (DRE gerencial, margem de contribuição por canal, comparação periódica e desempenho por empresa), sem sobrecarregar o banco de dados nem duplicar lógica financeira em controllers.

### Princípios Inegociáveis & Guardrails Anti-Alucinação:
1. **Não Duplicar Lógica Financeira em Controllers:**
   - Controllers HTTP atuam estritamente como portas de entrada REST read-only: sanitizam parâmetros, autenticam o tenant e delegam para o serviço canônico (`AggregatedKpiService`).
   - Todos os cálculos monetários, rateios, proporções e margens são executados exclusivamente em Java com `BigDecimal` bancário determinístico (escala 4, arredondamento `HALF_EVEN`).
   - É categoricamente proibido calcular valores agregados via tipos de ponto flutuante (`float`/`double`) ou via modelos de LLM.
2. **Isolamento Absoluto de Cache por Tenant (Zero Cross-Tenant Cache Contamination):**
   - O cache gerado para o Tenant Alpha jamais poderá ser lido, sobrescrito ou invalidado por requisições do Tenant Beta.
   - Toda chave de cache utiliza a infraestrutura canônica `TenantCacheKeyGenerator`:
     `tenant:{tenantId}:kpis:{aggregation_type}:{filter_hash}`.
   - A invalidação de cache opera em nível granular por tenant (`invalidateTenant(tenantId)`), mantendo intocados os caches de outros tenants no ecossistema multi-tenant.
3. **Catálogo Oficial e Documentação Rigorosa de Cada KPI:**
   - Cada métrica agregada possui definição matemática, fiscal e operacional catalogada, com rotulagem explícita de proveniência (`OBSERVED`, `IMPORTED`, `ESTIMATED`, `UNAVAILABLE`).
   - Permanece categoricamente proibido rotular ou apresentar a Margem de Contribuição como "Lucro Líquido", enquanto despesas fixas corporativas (folha, aluguel, infraestrutura) não estiverem modeladas.
4. **Invalidação Reativa em Eventos Relevantes:**
   - O cache do tenant é invalidado automaticamente através do mecanismo de eventos do Spring (`@EventListener` para `FinancialFactMutatedEvent` e mutações de custos/taxas), garantindo que extratos recém-conciliados ou pedidos reprocessados reflitam imediatamente nos relatórios sem stale data persistente.
5. **Performance e SLA Medido (RNF-004):**
   - Latência agregada sob carga com p95 estritamente menor que 500 ms (meta < 100 ms sob cache hit).

---

## 2. Catálogo Oficial e Definições de KPIs

| KPI Code | Nome do KPI | Descrição Operacional & Contábil | Fórmula Determinística | Unidade | Proveniência |
| :--- | :--- | :--- | :--- | :---: | :---: |
| `GROSS_REVENUE` | Receita Bruta | Soma dos valores brutos dos produtos vendidos nos pedidos faturados do período. | $\sum \text{grossRevenue}$ | BRL | `OBSERVED` / `IMPORTED` |
| `DISCOUNT_AMOUNT` | Descontos Concedidos | Total de abatimentos, cupons e descontos promocionais aplicados aos itens. | $\sum \text{discountAmount}$ | BRL | `OBSERVED` / `IMPORTED` |
| `NET_REVENUE` | Receita Líquida | Faturamento líquido efetivo após dedução dos descontos comerciais. | $\text{GrossRevenue} - \text{DiscountAmount}$ | BRL | `DETERMINISTIC` |
| `CANCELLATION_AMOUNT`| Cancelamentos Brutos | Montante financeiro de pedidos formalmente cancelados no período. | $\sum \text{cancellationAmount}$ | BRL | `OBSERVED` / `IMPORTED` |
| `RETURN_AMOUNT` | Devoluções Brutas | Montante financeiro de itens devolvidos/reembolsados no período. | $\sum \text{returnAmount}$ | BRL | `OBSERVED` / `IMPORTED` |
| `MARKETPLACE_FEES` | Comissões de Canal | Tarifas percentuais cobradas pelos marketplaces sobre os itens faturados. | $\sum \text{marketplaceFee}$ | BRL | `DETERMINISTIC` / `IMPORTED` |
| `FIXED_FEES` | Tarifas Fixas por Item | Tarifas fixas de transação/anúncio cobradas pelos marketplaces. | $\sum \text{fixedFee}$ | BRL | `DETERMINISTIC` / `IMPORTED` |
| `SHIPPING_COST` | Custo de Envio (Frete) | Custo logístico de frete cobrado do vendedor pelos canais ou transportadoras. | $\sum \text{shippingCost}$ | BRL | `OBSERVED` / `IMPORTED` |
| `PRODUCT_COST` | Custo dos Produtos (CMV)| Custo das Mercadorias Vendidas com base no custo unitário cadastrado na data. | $\sum (\text{qty} \times \text{unitProductCost})$ | BRL | `IMPORTED` (Custo Seller) |
| `PACKAGING_COST` | Custo de Embalagem | Custos de caixas, envelopes e insumos de expedição cadastrados. | $\sum \text{packagingCost}$ | BRL | `IMPORTED` (Custo Seller) |
| `ADDITIONAL_COST` | Custos Adicionais | Custos operacionais extras atrelados ao manuseio ou expedição. | $\sum \text{additionalCost}$ | BRL | `IMPORTED` (Custo Seller) |
| `TAX_AMOUNT` | Tributos Declarados | Carga tributária informada ou estimada com base no enquadramento tributário. | $\sum \text{taxAmount}$ | BRL | `USER_INPUT` / `ESTIMATED` |
| `OTHER_EXPENSES` | Outras Despesas | Despesas operacionais diversas atreladas diretamente aos pedidos. | $\sum \text{otherExpensesAmount}$ | BRL | `USER_INPUT` |
| `TOTAL_DEDUCTIONS` | Total de Deduções | Consolidação de custos variáveis, taxas, frete e tributos incidentes. | $\sum (\text{Taxas} + \text{Frete} + \text{CMV} + \text{Embalagem} + \text{Imposto} + \dots)$ | BRL | `DETERMINISTIC` |
| `CONTRIBUTION_MARGIN`| Margem de Contribuição| Sobra financeira após dedução de todos os custos e despesas variáveis diretas. | $\text{NetRevenue} - \text{TotalDeductions}$ | BRL | `DETERMINISTIC` |
| `CONTRIBUTION_MARGIN_RATIO`| Margem de Contribuição %| Eficiência percentual de retenção de margem sobre o faturamento líquido. | $(\text{ContributionMargin} / \text{NetRevenue}) \times 100$ | % | `DETERMINISTIC` |
| `AVERAGE_ORDER_VALUE`| Ticket Médio | Valor médio faturado por pedido faturado no período selecionado. | $\text{GrossRevenue} / \text{TotalOrders}$ | BRL | `DETERMINISTIC` |
| `TOTAL_ORDERS` | Total de Pedidos | Quantidade total de pedidos considerados na agregação analítica. | $\text{count}(\text{orders})$ | Int | `OBSERVED` |
| `RECONCILIATION_RATE`| Taxa de Conciliação | Percentual de pedidos com extrato de liquidação externa conferido. | $(\text{ReconciledOrders} / \text{TotalOrders}) \times 100$ | % | `OBSERVED` |

> **Aviso Legal Anti-Alucinação Mandatório:**  
> A Margem de Contribuição reflete a sobra após a cobertura dos custos diretos e variáveis dos produtos e das vendas. Ela **NÃO** deve ser interpretada como Lucro Líquido contábil, uma vez que despesas fixas da estrutura corporativa (salários, aluguel, depreciação, ferramentas e pró-labore) não são rateadas a nível de pedido na Seller Intelligence Platform.

---

## 3. Arquitetura dos Endpoints Agregados

### 3.1 Endpoints REST (`AggregatedKpiController.java`)

1. **`GET /api/v1/analytics/kpis/summary`**
   - **Objetivo:** Resumo completo e consolidado de KPIs financeiros do tenant com filtros combináveis.
   - **Parâmetros de Consulta:**
     * `companyId` (UUID, opcional)
     * `marketplace` (String, opcional: `MERCADO_LIVRE`, `SHOPEE`)
     * `sellerAccountId` (UUID, opcional)
     * `startDate` (Instant ISO 8601, opcional)
     * `endDate` (Instant ISO 8601, opcional)
   - **Resposta:** `AggregatedKpiSummaryDto` com metadados de cache, timestamp de cálculo e catálogo de definições.

2. **`GET /api/v1/analytics/kpis/by-channel`**
   - **Objetivo:** Agregação comparativa de rentabilidade e volume agrupada por marketplace/canal.
   - **Parâmetros:** `companyId`, `startDate`, `endDate`.
   - **Resposta:** `List<ChannelKpiAggregateDto>` (Marketplace, Pedidos, Receita Bruta, Receita Líquida, Margem de Contribuição, Margem %, Participação na Receita %).

3. **`GET /api/v1/analytics/kpis/by-period`**
   - **Objetivo:** Agregação temporal para gráficos de tendência e evolução de margem.
   - **Parâmetros:** `companyId`, `marketplace`, `startDate`, `endDate`, `granularity` (`DAILY`, `WEEKLY`, `MONTHLY`).
   - **Resposta:** `List<PeriodicKpiAggregateDto>` (Chave do período, data início, data fim, Pedidos, Receita Bruta, Receita Líquida, Margem, Margem %).

4. **`GET /api/v1/analytics/kpis/by-company`**
   - **Objetivo:** Agregação comparativa por empresa / CNPJ da organização.
   - **Parâmetros:** `marketplace`, `startDate`, `endDate`.
   - **Resposta:** `List<CompanyKpiAggregateDto>` (Company ID, Pedidos, Receita Bruta, Margem de Contribuição).

5. **`GET /api/v1/analytics/kpis/definitions`**
   - **Objetivo:** Consulta do catálogo oficial documentado de definições de KPIs para enriquecer a UI.
   - **Resposta:** `List<KpiDefinitionDto>`.

6. **`POST /api/v1/analytics/kpis/cache/invalidate`**
   - **Objetivo:** Invalidação sob demanda do cache do tenant autenticado.
   - **Resposta:** HTTP 204 No Content.

---

## 4. Camada de Cache Tenant-Aware & Invalidação Reativa

### 4.1 Estrutura de Chave de Cache
A chave é formada por:
```
tenant:{tenantId}:kpis:{endpoint}:{normalized_filter_hash}
```
Exemplo:
```
tenant:11111111-1111-1111-1111-111111111111:kpis:summary:c8a2b5e3f1904a...
```

### 4.2 TTL e Política de Evicção
- **TTL Padrão:** 15 minutos (900 segundos).
- **Evicção Reativa por Evento (`KpiCacheInvalidationListener.java`):**
  - Escuta `FinancialFactMutatedEvent` emitido sempre que:
    * Um pedido tem seus fatos calculados ou reprocessados (`computeOrReprocessOrderFacts`).
    * Um fato é reconciliado com extrato externo (`reconcileWithExternalSettlement`).
  - Ao capturar o evento, o listener executa:
    ```java
    kpiCacheService.invalidateTenant(event.tenantId());
    ```
    Garantindo que nenhuma requisição subsequente receba dados desatualizados.

---

## 5. Critérios de Aceite (Definition of Acceptance)

1. **Agregações Conferem com Fontes:**
   - A soma ponderada dos fatos individuais no banco de dados (`OrderFinancialFactRecord`) confere centavo a centavo com os valores agregados em `BigDecimal` (`grossRevenue`, `contributionMargin`, `totalDeductions`).
2. **Cache Não Mistura Tenant:**
   - O cache gerado para o Tenant Alpha é inacessível para o Tenant Beta.
   - Invalidação no Tenant Alpha mantém íntegro o cache do Tenant Beta.
3. **p95 Medido:**
   - Teste automatizado de benchmark demonstra latência agregada p95 < 500 ms (meta < 100 ms em cache hit).
4. **Contrato Documentado:**
   - Todas as definições de KPIs estão catalogadas e acessíveis tanto na especificação quanto via endpoint `/api/v1/analytics/kpis/definitions`.
