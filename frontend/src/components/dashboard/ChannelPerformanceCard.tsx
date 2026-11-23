'use client';

import React from 'react';
import { ChannelKpiAggregate } from '@/types/kpis';
import { formatCurrency, formatPercent } from './ExecutiveKpiCards';

interface ChannelPerformanceCardProps {
  channels: ChannelKpiAggregate[];
  isLoading: boolean;
}

export const ChannelPerformanceCard: React.FC<ChannelPerformanceCardProps> = ({
  channels,
  isLoading,
}) => {
  if (isLoading) {
    return (
      <div
        role="status"
        aria-label="Carregando desempenho por canal"
        className="p-5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm animate-pulse space-y-4"
      >
        <span className="sr-only">Carregando desempenho por canal...</span>
        <div className="h-5 bg-slate-200 dark:bg-slate-800 rounded w-1/3" />
        <div className="space-y-3">
          <div className="h-16 bg-slate-100 dark:bg-slate-800/60 rounded" />
          <div className="h-16 bg-slate-100 dark:bg-slate-800/60 rounded" />
        </div>
      </div>
    );
  }

  const getMarketplaceLabel = (m: string) => {
    switch (m.toUpperCase()) {
      case 'MERCADO_LIVRE':
        return 'Mercado Livre';
      case 'SHOPEE':
        return 'Shopee';
      case 'AMAZON':
        return 'Amazon';
      default:
        return m;
    }
  };

  const getChannelColor = (m: string) => {
    switch (m.toUpperCase()) {
      case 'MERCADO_LIVRE':
        return 'bg-amber-500';
      case 'SHOPEE':
        return 'bg-orange-500';
      case 'AMAZON':
        return 'bg-blue-500';
      default:
        return 'bg-indigo-500';
    }
  };

  return (
    <article
      aria-label="Desempenho por Canal de Venda"
      className="p-5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm flex flex-col justify-between"
    >
      <div>
        <div className="flex items-center justify-between pb-3 mb-4 border-b border-slate-100 dark:border-slate-800">
          <div>
            <h3 className="font-semibold text-slate-900 dark:text-white text-sm">
              Desempenho por Canal
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Distribuição de receita e margem entre marketplaces conectados
            </p>
          </div>
          <span className="text-xs font-medium px-2 py-0.5 rounded bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300">
            {channels.length} {channels.length === 1 ? 'canal' : 'canais'}
          </span>
        </div>

        {channels.length === 0 ? (
          <div className="py-8 text-center text-slate-500 dark:text-slate-400 text-xs">
            Nenhum dado por canal encontrado no período selecionado.
          </div>
        ) : (
          <div className="space-y-4">
            {channels.map((chan) => (
              <div
                key={chan.marketplace}
                data-testid={`channel-item-${chan.marketplace.toLowerCase()}`}
                className="p-3.5 rounded-lg border border-slate-100 dark:border-slate-800/80 bg-slate-50/50 dark:bg-slate-800/30 space-y-2.5"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className={`w-2.5 h-2.5 rounded-full ${getChannelColor(chan.marketplace)}`} />
                    <span className="font-semibold text-xs text-slate-900 dark:text-white">
                      {getMarketplaceLabel(chan.marketplace)}
                    </span>
                    <span className="text-[11px] text-slate-500 dark:text-slate-400">
                      ({chan.orderCount} pedidos)
                    </span>
                  </div>
                  <span className="text-xs font-bold text-slate-900 dark:text-white">
                    {formatCurrency(chan.grossRevenue)}
                  </span>
                </div>

                {/* Barra de Progresso de Share */}
                <div className="space-y-1">
                  <div className="flex justify-between text-[11px] text-slate-500 dark:text-slate-400">
                    <span>Participação na Receita</span>
                    <span className="font-medium text-slate-700 dark:text-slate-300">
                      {formatPercent(chan.revenueSharePercent)}
                    </span>
                  </div>
                  <div className="w-full h-1.5 bg-slate-200 dark:bg-slate-700 rounded-full overflow-hidden">
                    <div
                      className={`h-full rounded-full ${getChannelColor(chan.marketplace)}`}
                      style={{ width: `${Math.min(Math.max(chan.revenueSharePercent, 0), 100)}%` }}
                    />
                  </div>
                </div>

                {/* Métricas secundárias */}
                <div className="grid grid-cols-2 pt-1 border-t border-slate-200/60 dark:border-slate-800 text-[11px] text-slate-600 dark:text-slate-400">
                  <div>
                    Margem: <strong className="text-teal-800 dark:text-teal-300">{formatCurrency(chan.contributionMarginAmount)}</strong>
                  </div>
                  <div className="text-right">
                    Margem %: <strong className="text-slate-900 dark:text-white">{formatPercent(chan.averageContributionMarginRatio)}</strong>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </article>
  );
};
