import React from 'react';
import { AiUsageMetricsDto } from '@/types/aiassistant';

interface AiUsageMetricsPillProps {
  metrics: AiUsageMetricsDto;
  className?: string;
}

export const AiUsageMetricsPill: React.FC<AiUsageMetricsPillProps> = ({ metrics, className = '' }) => {
  const formattedCost = new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    minimumFractionDigits: 4,
    maximumFractionDigits: 6,
  }).format(metrics.estimatedCostBrl);

  return (
    <div
      className={`inline-flex flex-wrap items-center gap-2 px-2.5 py-1 text-xs font-mono rounded-lg bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700 ${className}`}
      data-testid="ai-usage-metrics-pill"
    >
      <span className="flex items-center gap-1 font-semibold text-indigo-600 dark:text-indigo-400">
        <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
        </svg>
        {metrics.totalTokens.toLocaleString('pt-BR')} tokens
      </span>
      <span className="text-slate-400 dark:text-slate-500">•</span>
      <span title={`Entrada: ${metrics.promptTokens} | Saída: ${metrics.completionTokens}`}>
        {metrics.latencyMs}ms
      </span>
      <span className="text-slate-400 dark:text-slate-500">•</span>
      <span className="font-medium text-emerald-600 dark:text-emerald-400" title="Custo financeiro estimado">
        {formattedCost}
      </span>
    </div>
  );
};
