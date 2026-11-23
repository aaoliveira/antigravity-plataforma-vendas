'use client';

import React from 'react';

interface StaleDataBannerProps {
  calculatedAt?: string;
  isCached: boolean;
  isRefreshing: boolean;
  onRefresh: () => void;
}

export const StaleDataBanner: React.FC<StaleDataBannerProps> = ({
  calculatedAt,
  isCached,
  isRefreshing,
  onRefresh,
}) => {
  const formattedDate = calculatedAt
    ? new Date(calculatedAt).toLocaleString('pt-BR', {
        dateStyle: 'short',
        timeStyle: 'medium',
      })
    : 'Agora';

  return (
    <div
      role="region"
      aria-label="Status de Sincronização do Dashboard"
      className="flex flex-wrap items-center justify-between gap-2 p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900/60 text-xs"
    >
      <div className="flex items-center gap-2">
        <span
          className={`w-2 h-2 rounded-full ${
            isCached ? 'bg-amber-500 animate-pulse' : 'bg-emerald-500'
          }`}
        />
        <span className="text-slate-600 dark:text-slate-400">
          Última apuração calculada em: <strong className="text-slate-900 dark:text-white font-semibold">{formattedDate}</strong>
        </span>
        <span
          className={`px-2 py-0.5 rounded text-[10px] font-semibold tracking-wide ${
            isCached
              ? 'bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300'
              : 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300'
          }`}
        >
          {isCached ? 'Cache Ativo (15m TTL)' : 'Tempo Real'}
        </span>
      </div>

      <button
        type="button"
        onClick={onRefresh}
        disabled={isRefreshing}
        aria-label="Atualizar dados agora e invalidar cache"
        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-700 text-xs font-medium transition-colors disabled:opacity-50 min-h-[36px]"
      >
        <svg
          className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin' : ''}`}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
          />
        </svg>
        {isRefreshing ? 'Atualizando...' : 'Atualizar Dados'}
      </button>
    </div>
  );
};
