'use client';

import React from 'react';
import { PeriodicKpiAggregate, KpiGranularity } from '@/types/kpis';
import { formatCurrency } from './ExecutiveKpiCards';

interface PeriodicTrendChartProps {
  periods: PeriodicKpiAggregate[];
  granularity: KpiGranularity;
  onGranularityChange: (gran: KpiGranularity) => void;
  isLoading: boolean;
}

export const PeriodicTrendChart: React.FC<PeriodicTrendChartProps> = ({
  periods,
  granularity,
  onGranularityChange,
  isLoading,
}) => {
  if (isLoading) {
    return (
      <div
        role="status"
        aria-label="Carregando tendência periódica"
        className="p-5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm animate-pulse space-y-4"
      >
        <span className="sr-only">Carregando série periódica...</span>
        <div className="h-5 bg-slate-200 dark:bg-slate-800 rounded w-1/3" />
        <div className="h-44 bg-slate-100 dark:bg-slate-800/60 rounded" />
      </div>
    );
  }

  const maxRevenue = Math.max(...periods.map((p) => p.grossRevenue), 1);

  return (
    <article
      aria-label="Evolução Temporal e Tendência Periódica"
      className="p-5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm flex flex-col justify-between"
    >
      <div>
        <div className="flex flex-wrap items-center justify-between gap-2 pb-3 mb-4 border-b border-slate-100 dark:border-slate-800">
          <div>
            <h3 className="font-semibold text-slate-900 dark:text-white text-sm">
              Evolução Temporal
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Receita Bruta e Margem de Contribuição por período
            </p>
          </div>

          {/* Granularidade */}
          <div
            role="group"
            aria-label="Granularidade temporal"
            className="inline-flex rounded-lg border border-slate-200 dark:border-slate-700 p-0.5 bg-slate-50 dark:bg-slate-800"
          >
            {(['DAILY', 'WEEKLY', 'MONTHLY'] as KpiGranularity[]).map((g) => (
              <button
                key={g}
                type="button"
                onClick={() => onGranularityChange(g)}
                aria-pressed={granularity === g}
                className={`px-2.5 py-1 text-xs font-medium rounded-md transition-colors min-h-[32px] ${
                  granularity === g
                    ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-white shadow-sm font-semibold'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                }`}
              >
                {g === 'DAILY' ? 'Dia' : g === 'WEEKLY' ? 'Semana' : 'Mês'}
              </button>
            ))}
          </div>
        </div>

        {periods.length === 0 ? (
          <div className="py-12 text-center text-slate-500 dark:text-slate-400 text-xs">
            Nenhum dado periódico encontrado no intervalo.
          </div>
        ) : (
          <div className="space-y-3">
            {/* Gráfico de barras simples e responsivo em CSS */}
            <div className="pt-2 pb-4 overflow-x-auto">
              <div className="flex items-end gap-2 min-w-[280px] h-36">
                {periods.map((p) => {
                  const barHeightPct = Math.max((p.grossRevenue / maxRevenue) * 100, 4);
                  return (
                    <div
                      key={p.periodLabel}
                      data-testid={`trend-bar-${p.periodLabel}`}
                      className="flex-1 flex flex-col items-center justify-end h-full group relative"
                    >
                      {/* Tooltip Hover */}
                      <div className="absolute -top-12 z-20 hidden group-hover:flex flex-col items-center bg-slate-900 text-white dark:bg-slate-100 dark:text-slate-900 text-[10px] py-1 px-2 rounded shadow-md pointer-events-none whitespace-nowrap">
                        <span>{p.periodLabel}: {formatCurrency(p.grossRevenue)}</span>
                        <span>Margem: {formatCurrency(p.contributionMarginAmount)}</span>
                      </div>

                      {/* Barra */}
                      <div className="w-full max-w-[36px] bg-slate-100 dark:bg-slate-800 rounded-t overflow-hidden flex flex-col justify-end h-full">
                        <div
                          className="w-full bg-blue-500 dark:bg-blue-600 rounded-t transition-all duration-300 hover:bg-blue-600 dark:hover:bg-blue-500"
                          style={{ height: `${barHeightPct}%` }}
                        />
                      </div>
                      <span className="text-[10px] text-slate-500 dark:text-slate-400 mt-1 truncate max-w-[48px]">
                        {p.periodLabel}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Legenda */}
            <div className="flex items-center justify-end gap-4 text-[11px] text-slate-500 dark:text-slate-400 pt-2 border-t border-slate-100 dark:border-slate-800">
              <span className="flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded bg-blue-500" />
                Receita Bruta
              </span>
            </div>
          </div>
        )}
      </div>
    </article>
  );
};
