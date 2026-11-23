'use client';

import React from 'react';
import { AggregatedKpiSummary } from '@/types/kpis';

interface ExecutiveKpiCardsProps {
  summary: AggregatedKpiSummary | null;
  isLoading: boolean;
  onOpenDefinitions?: (kpiCode?: string) => void;
}

export const formatCurrency = (val: number | undefined | null): string => {
  if (val === undefined || val === null || isNaN(val)) return 'R$ 0,00';
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val);
};

export const formatPercent = (val: number | undefined | null): string => {
  if (val === undefined || val === null || isNaN(val)) return '0,00%';
  return `${val.toFixed(2).replace('.', ',')}%`;
};

export const ExecutiveKpiCards: React.FC<ExecutiveKpiCardsProps> = ({
  summary,
  isLoading,
  onOpenDefinitions,
}) => {
  if (isLoading) {
    return (
      <section
        aria-label="Carregando indicadores principais"
        role="status"
        className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4"
      >
        <span className="sr-only">Carregando indicadores executivos...</span>
        {Array.from({ length: 6 }).map((_, idx) => (
          <div
            key={idx}
            className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm animate-pulse space-y-3"
          >
            <div className="h-4 bg-slate-200 dark:bg-slate-800 rounded w-2/3" />
            <div className="h-7 bg-slate-300 dark:bg-slate-700 rounded w-5/6" />
            <div className="h-3 bg-slate-200 dark:bg-slate-800 rounded w-1/2" />
          </div>
        ))}
      </section>
    );
  }

  const cards = [
    {
      code: 'TOTAL_ORDERS',
      title: 'Pedidos Faturados',
      value: (summary?.totalOrders ?? 0).toLocaleString('pt-BR'),
      subtext: `${summary?.reconciledCount ?? 0} reconciliados`,
      badgeColor: 'bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300',
    },
    {
      code: 'GROSS_REVENUE',
      title: 'Receita Bruta Total',
      value: formatCurrency(summary?.totalGrossRevenue),
      subtext: `Descontos: ${formatCurrency(summary?.totalDiscountAmount)}`,
      badgeColor: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300',
    },
    {
      code: 'NET_REVENUE',
      title: 'Receita Líquida',
      value: formatCurrency(summary?.totalNetRevenue),
      subtext: `Cancel/Devol: ${formatCurrency((summary?.totalCancellationAmount ?? 0) + (summary?.totalReturnAmount ?? 0))}`,
      badgeColor: 'bg-indigo-100 text-indigo-800 dark:bg-indigo-900/40 dark:text-indigo-300',
    },
    {
      code: 'CONTRIBUTION_MARGIN',
      title: 'Margem Contribuição',
      value: formatCurrency(summary?.totalContributionMarginAmount),
      subtext: `Margem média: ${formatPercent(summary?.averageContributionMarginRatio)}`,
      badgeColor: 'bg-teal-100 text-teal-800 dark:bg-teal-900/40 dark:text-teal-300',
      isHighlight: true,
    },
    {
      code: 'AVERAGE_ORDER_VALUE',
      title: 'Ticket Médio (AOV)',
      value: formatCurrency(summary?.averageOrderValue),
      subtext: 'Receita bruta por pedido',
      badgeColor: 'bg-purple-100 text-purple-800 dark:bg-purple-900/40 dark:text-purple-300',
    },
    {
      code: 'RECONCILIATION_RATE',
      title: 'Taxa de Reconciliação',
      value: formatPercent(summary?.reconciliationRatePercent),
      subtext: `${summary?.estimatedCount ?? 0} pendentes / estimadas`,
      badgeColor:
        (summary?.reconciliationRatePercent ?? 0) >= 90
          ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300'
          : 'bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300',
    },
  ];

  return (
    <section aria-label="Indicadores Executivos" className="space-y-4">
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
        {cards.map((card) => (
          <article
            key={card.code}
            className={`p-4 rounded-xl border transition-all duration-200 relative group flex flex-col justify-between ${
              card.isHighlight
                ? 'border-teal-300 dark:border-teal-700/60 bg-teal-50/40 dark:bg-teal-950/20 shadow-sm'
                : 'border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm hover:border-slate-300 dark:hover:border-slate-700'
            }`}
          >
            <div className="flex items-start justify-between gap-1 mb-2">
              <h3 className="text-xs font-semibold text-slate-600 dark:text-slate-400 uppercase tracking-wider line-clamp-1">
                {card.title}
              </h3>
              {onOpenDefinitions && (
                <button
                  type="button"
                  onClick={() => onOpenDefinitions(card.code)}
                  aria-label={`Ver fórmula e definição de ${card.title}`}
                  className="text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 p-1 rounded hover:bg-slate-100 dark:hover:bg-slate-800 min-h-[32px] min-w-[32px] flex items-center justify-center transition-colors"
                  title="Ver definição e fórmula do KPI"
                >
                  <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 018 0z"
                    />
                  </svg>
                </button>
              )}
            </div>

            <div className="space-y-1">
              <p
                data-testid={`kpi-value-${card.code.toLowerCase()}`}
                className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-white tracking-tight break-words"
              >
                {card.value}
              </p>
              <p className="text-xs text-slate-500 dark:text-slate-400 font-medium">
                {card.subtext}
              </p>
            </div>
          </article>
        ))}
      </div>

      {/* Detalhamento de Deduções e Custos Diretos */}
      {summary && (
        <details className="group border border-slate-200 dark:border-slate-800 rounded-xl bg-white dark:bg-slate-900 overflow-hidden text-xs">
          <summary className="px-4 py-3 cursor-pointer select-none font-semibold text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800/50 flex items-center justify-between min-h-[44px]">
            <span className="flex items-center gap-2">
              <svg className="w-4 h-4 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
              </svg>
              Detalhamento de Deduções, CMV e Despesas Variáveis
            </span>
            <span className="text-slate-500 font-normal">
              Total: <strong className="text-slate-900 dark:text-white font-semibold">{formatCurrency(summary.totalDeductions)}</strong>
            </span>
          </summary>
          <div className="p-4 pt-2 border-t border-slate-100 dark:border-slate-800 grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
            <div className="p-2 rounded bg-slate-50 dark:bg-slate-800/40">
              <span className="text-slate-500 dark:text-slate-400 block text-[11px]">Comissões Canal:</span>
              <span className="font-semibold text-slate-900 dark:text-white">{formatCurrency(summary.totalMarketplaceFees)}</span>
            </div>
            <div className="p-2 rounded bg-slate-50 dark:bg-slate-800/40">
              <span className="text-slate-500 dark:text-slate-400 block text-[11px]">Taxas Fixas:</span>
              <span className="font-semibold text-slate-900 dark:text-white">{formatCurrency(summary.totalFixedFees)}</span>
            </div>
            <div className="p-2 rounded bg-slate-50 dark:bg-slate-800/40">
              <span className="text-slate-500 dark:text-slate-400 block text-[11px]">Fretes Pagos:</span>
              <span className="font-semibold text-slate-900 dark:text-white">{formatCurrency(summary.totalShippingCost)}</span>
            </div>
            <div className="p-2 rounded bg-slate-50 dark:bg-slate-800/40">
              <span className="text-slate-500 dark:text-slate-400 block text-[11px]">CMV (Produtos):</span>
              <span className="font-semibold text-slate-900 dark:text-white">{formatCurrency(summary.totalProductCost)}</span>
            </div>
            <div className="p-2 rounded bg-slate-50 dark:bg-slate-800/40">
              <span className="text-slate-500 dark:text-slate-400 block text-[11px]">Embalagem & Adic.:</span>
              <span className="font-semibold text-slate-900 dark:text-white">
                {formatCurrency((summary.totalPackagingCost ?? 0) + (summary.totalAdditionalCost ?? 0))}
              </span>
            </div>
            <div className="p-2 rounded bg-slate-50 dark:bg-slate-800/40">
              <span className="text-slate-500 dark:text-slate-400 block text-[11px]">Impostos Estimados:</span>
              <span className="font-semibold text-slate-900 dark:text-white">{formatCurrency(summary.totalTaxAmount)}</span>
            </div>
          </div>
        </details>
      )}
    </section>
  );
};
