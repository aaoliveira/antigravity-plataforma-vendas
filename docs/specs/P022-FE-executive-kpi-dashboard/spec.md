# Especificação Técnica: P022-FE — Dashboard Executivo com KPIs, Desempenho por Canal, Sincronização e Alertas de Qualidade

> **Versão:** 1.0.0 &middot; **Status:** EM IMPLEMENTAÇÃO &middot; **Data:** 23/11/2026  
> **Camada:** Frontend (Next.js 15 App Router / React 19 / Tailwind CSS / TypeScript)  
> **Backend Correspondente:** P022-BE (`AggregatedKpiController` sob `/api/v1/analytics/kpis/*`)  
> **Critérios de Aceite:** Números batem com API; tema persiste (Light mode padrão, Dark mode opcional); breakpoints testados (360px a desktop); WCAG 2.1 AA nos fluxos principais; sem overflow.

---

## 1. Visão Geral e Justificativa

O prompt `P022-FE` entrega o **Dashboard Executivo Analítico** que consolida de ponta a ponta as informações financeiras e operacionais da organização vendedora. Consumindo os endpoints agregados read-only com cache tenant-aware construídos em `P022-BE`, a interface oferece:
1. **Cards de KPIs Executivos:** Pedidos, Receita Bruta, Receita Líquida, Margem de Contribuição (R$ e %), Ticket Médio e Taxa de Reconciliação, reconciliando com as fontes bancárias e contábeis.
2. **Desempenho Comparativo por Canal:** Quebra por marketplace (Mercado Livre, Shopee) com cálculo do share de faturamento e margem.
3. **Tendência Periódica Temporal:** Série temporal com seletor de granularidade (`DAILY`, `WEEKLY`, `MONTHLY`).
4. **Alertas de Qualidade dos Dados & Sincronização:** Indicadores visuais claros de integridade, contagem de pedidos reconciliados vs estimados vs divergentes, e alerta para pedidos com dados ausentes.
5. **Aviso de Stale Data e Invalidação Sob Demanda:** Sinalização visual quando os dados servidos provêm do cache e botão de sincronização forçada (`POST /api/analytics/kpis/cache/invalidate`).
6. **Catálogo Documentado de Definições:** Gaveta lateral acessível exibindo as fórmulas e a proveniência dos 18 KPIs.
7. **Salvaguarda Anti-Alucinação:** Exibição permanente do disclaimer legal e contábil proibindo o rótulo de Lucro Líquido.
8. **Temas e Responsividade:** Light mode padrão, dark mode persistido em `localStorage`, e adaptação sem overflow horizontal de 360 px a telas desktop ultrawide.

---

## 2. Contratos de Dados & Interfaces TypeScript

### 2.1 Estrutura de Tipos (`frontend/src/types/kpis.ts`)

```typescript
export interface KpiDefinition {
  code: string;
  name: string;
  formula: string;
  description: string;
  unit: string;
  provenance: string;
}

export interface AggregatedKpiSummary {
  tenantId: string;
  totalOrders: number;
  totalGrossRevenue: number;
  totalDiscountAmount: number;
  totalNetRevenue: number;
  totalCancellationAmount: number;
  totalReturnAmount: number;
  totalMarketplaceFees: number;
  totalFixedFees: number;
  totalShippingCost: number;
  totalProductCost: number;
  totalPackagingCost: number;
  totalAdditionalCost: number;
  totalTaxAmount: number;
  totalOtherExpensesAmount: number;
  totalDeductions: number;
  totalContributionMarginAmount: number;
  averageContributionMarginRatio: number;
  averageOrderValue: number;
  reconciledCount: number;
  estimatedCount: number;
  divergentCount: number;
  ordersWithMissingDataCount: number;
  reconciliationRatePercent: number;
  cached: boolean;
  calculatedAt: string;
  antiHallucinationDisclaimer: string;
  kpiDefinitions?: KpiDefinition[];
}

export interface ChannelKpiAggregate {
  marketplace: string;
  orderCount: number;
  grossRevenue: number;
  netRevenue: number;
  totalDeductions: number;
  contributionMarginAmount: number;
  averageContributionMarginRatio: number;
  revenueSharePercent: number;
  marginSharePercent: number;
}

export interface PeriodicKpiAggregate {
  periodLabel: string;
  orderCount: number;
  grossRevenue: number;
  netRevenue: number;
  contributionMarginAmount: number;
  averageOrderValue: number;
}

export interface CompanyKpiAggregate {
  companyId: string;
  companyName: string;
  cnpj: string;
  orderCount: number;
  grossRevenue: number;
  netRevenue: number;
  contributionMarginAmount: number;
  revenueSharePercent: number;
}

export type KpiGranularity = 'DAILY' | 'WEEKLY' | 'MONTHLY';
export type DashboardPeriodPreset = 'LAST_7' | 'LAST_30' | 'LAST_90' | 'CUSTOM';
```

---

## 3. Arquitetura de Componentes & Estados

```
frontend/src/components/dashboard/
├── ExecutiveDashboardView.tsx        # View principal conectando filtros, fetchers e estados
├── ExecutiveKpiCards.tsx             # 6 cartões executivos de KPI com skeleton e tooltips
├── ChannelPerformanceCard.tsx        # Card comparativo por marketplace com barras de share
├── PeriodicTrendChart.tsx            # Gráfico de barras e tendência periódica com granularidade
├── DataQualityAlerts.tsx             # Alertas de qualidade de dados (reconciliação e faltantes)
├── StaleDataBanner.tsx               # Banner de sincronização, cache status e atualização imediata
└── KpiDefinitionsDrawer.tsx          # Gaveta lateral com fórmulas e glossário de KPIs
```

### 3.1 Tratamento Uniforme dos 4 Estados Obrigatórios
- **Skeleton:** Renderiza placeholders animados (`animate-pulse`) enquanto as requisições estão em trânsito.
- **Empty State:** Renderizado com elegância quando `totalOrders === 0` com orientação para importar canais/pedidos.
- **Error State:** Banner RFC 7807 com mensagem de erro amigável e botão "Tentar Novamente".
- **Stale Data:** Indicador de cache expirado/antigo com badge e acionador de revalidação forçada.

---

## 4. Requisitos de Acessibilidade (WCAG 2.1 AA) e Responsividade

1. **Acessibilidade:**
   - Contraste mínimo de 4.5:1 nos modos claro e escuro.
   - Atributos ARIA adequados: `role="region"`, `role="status"`, `role="alert"`, `aria-label`, `aria-expanded`.
   - Navegação por teclado: Tecla `Escape` fecha a gaveta de definições; foco visível em todos os seletores e botões.
   - Touch targets $\ge 44$ px em telas móveis.

2. **Responsividade & Zero Overflow:**
   - Mobile ($360$ px a $640$ px): coluna única, `w-full`, quebras de texto automáticas, sem scroll horizontal (`overflow-x-hidden`).
   - Tablet ($641$ px a $1023$ px): grid de 2 a 3 colunas para KPIs.
   - Desktop ($\ge 1024$ px): grid de 3 a 6 colunas, layout balanceado com visão comparativa.

3. **Anti-Alucinação:**
   - Exibição visível e inamovível do aviso contábil de que o demonstrativo apura estritamente Margem de Contribuição, vedando a identificação como Lucro Líquido sem custos fixos estruturais.
