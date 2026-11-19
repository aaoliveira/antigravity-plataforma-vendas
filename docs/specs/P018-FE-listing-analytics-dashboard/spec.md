# Especificação Técnica: P018-FE - Dashboard de Analytics por Anúncio e Período

## 1. Contexto & Objetivos

Esta especificação define o frontend do **P018-FE**, construído sobre os contratos e serviços oficiais entregues no **P018-BE**.
O objetivo é fornecer ao vendedor um dashboard completo e rigoroso de inteligência analítica de desempenho de anúncios e contas conectadas, cobrindo:
- **Período e Timezone Explícitos:** Seleção de intervalo com indicação visual inconfundível do fuso horário de referência (`America/Sao_Paulo (UTC-03:00)`) e janelas UTC normalizadas.
- **Comparação Temporal Determinística:** Cálculo de $\Delta$ relativo (%) e absoluto frente ao período anterior imediatamente precedente de igual duração, com aviso explícito quando o período anterior não tiver dados coletados.
- **Anti-Alucinação Estrita & Aviso de Indisponibilidade:** Identificação clara de métricas não suportadas por canal (`UNSUPPORTED_BY_CHANNEL`), como o CTR de tráfego orgânico no Mercado Livre ou métricas privadas de concorrentes, e métricas com dados insuficientes (`INSUFFICIENT_DATA`), sem jamais inventar dados ou estimar silenciosamente.
- **Gráficos Sem Mascaramento:** Séries temporais que identificam explicitamente lacunas (missing dates / gaps), proibindo a interpolação contínua falsa ou atribuição de valor zero a dias sem coleta.

---

## 2. Critérios de Aceite (Definition of Acceptance)

1. **Período e Timezone Explícitos:**
   - A interface exibe em destaque o fuso horário ativo (`America/Sao_Paulo (UTC-03:00)`).
   - Datas de início e fim da janela são apresentadas no formato local amigável e com marcação de intervalo UTC na auditoria.
   - Presets rápidos disponíveis: Hoje, Últimos 7 dias, Últimos 14 dias, Últimos 30 dias, Mês Atual e Intervalo Personalizado.
2. **Cálculos Conferem:**
   - Fórmulas oficiais determinísticas exibidas via tooltip/modal:
     $$\text{CTR} = \frac{\text{clicks}}{\text{impressions}} \times 100$$
     $$\text{Conversão (Pedidos)} = \frac{\text{orders\_count}}{\text{visits}} \times 100$$
     $$\text{Conversão (Unidades)} = \frac{\text{units\_sold}}{\text{visits}} \times 100$$
   - Variação temporal:
     $$\Delta\% = \frac{\text{Valor Atual} - \text{Valor Anterior}}{\text{Valor Anterior}} \times 100$$
   - Tratamento de denominador zero: quando o valor anterior for zero ou nulo, o dashboard indica "Sem base anterior" em vez de $+100\%$ ou `NaN`.
3. **Gráficos Não Mascaram Dados Ausentes:**
   - Gráfico de tendência diária sinaliza visualmente dias sem observação ou lacunas de coleta.
   - Proibido conectar linhas entre pontos distantes como se o intervalo tivesse dados reais.
   - Proibido plotar dias não coletados como 0.
4. **Transparência de Métricas Indisponíveis:**
   - Cada KPI exibe badge de status: `AVAILABLE`, `UNAVAILABLE`, `INSUFFICIENT_DATA`, `UNSUPPORTED_BY_CHANNEL`.
   - Tooltip explicativo com motivo oficial de indisponibilidade (ex.: limitações do Mercado Livre Orgânico).
5. **Responsividade e Acessibilidade:**
   - Touch targets $\ge 44$px para mobile.
   - Navegação completa por teclado e atributos ARIA (`role="region"`, `aria-label`, `aria-expanded`).

---

## 3. Arquitetura da Solução

### 3.1 Tipos TypeScript (`frontend/src/types/metrics.ts`)
- `MetricScope`: `'ACCOUNT' | 'LISTING'`
- `MetricGranularity`: `'DAILY' | 'PERIOD_TOTAL'`
- `MetricAvailabilityStatus`: `'AVAILABLE' | 'UNAVAILABLE' | 'INSUFFICIENT_DATA' | 'UNSUPPORTED_BY_CHANNEL'`
- `MetricProvenance`: `'OFFICIAL_API' | 'DERIVED_FORMULA' | 'MANUAL_IMPORT'`
- `MetricUnit`: `'COUNT' | 'CURRENCY' | 'PERCENTAGE' | 'RATIO'`
- `AccountMetricDto`: espelho exato do DTO do backend Java.
- `MetricsSummaryDto`: totais e taxas consolidadas.
- `MetricCatalogEntry`: especificação de métricas do canal, rate limits e limitações.
- `TemporalComparison`: comparação entre período corrente e anterior com $\Delta$ absoluto e percentual.

### 3.2 Rotas BFF Next.js (`frontend/src/app/api/metrics/`)
- `GET /api/metrics`: repassa filtros (`sellerAccountId`, `marketplace`, `externalListingId`, `scope`, `metricKey`, `status`, `startDate`, `endDate`) para o backend Spring `/api/v1/metrics`.
- `GET /api/metrics/summary`: repassa consulta de resumo consolidado para `/api/v1/metrics/summary`.
- `GET /api/metrics/catalog`: consulta o catálogo oficial em `/api/v1/metrics/catalog`.

### 3.3 Componentes React (`frontend/src/components/listinganalytics/`)
1. `ListingAnalyticsDashboardView.tsx`: container orquestrador com seleção de anúncio/conta, timezone explícito, presets de período e controle de comparação temporal.
2. `MetricKpiGrid.tsx`: grade de cards de KPIs com variações temporais ($\Delta$), status e botões de ajuda.
3. `MetricTimeSeriesChart.tsx`: gráfico acessível com demarcação explícita de lacunas de coleta.
4. `MetricCatalogDrawer.tsx`: painel deslizante com a matriz de capacidades oficiais do canal selecionado.
5. `ListingAnalyticsDetailTable.tsx`: listagem auditável das métricas individuais persistidas.

---

## 4. Plano de Testes

1. `ListingAnalyticsDashboardView.test.tsx`:
   - Renderização inicial com período e timezone explícitos.
   - Aplicação de filtros por anúncio e período.
   - Cálculo e exibição correta de deltas comparativos temporais.
   - Tratamento de métricas `UNSUPPORTED_BY_CHANNEL` com badge e mensagem anti-alucinação.
   - Exibição de lacunas de coleta no gráfico temporal sem interpolação falsa.
   - Abertura da gaveta de catálogo oficial.
   - Acessibilidade por teclado (Escape fecha drawer, botões com aria-label).
