'use client';

import React, { useState } from 'react';
import { AccountMetricDto, MetricAvailabilityStatus } from '@/types/metrics';

export interface ListingAnalyticsDetailTableProps {
  metrics: AccountMetricDto[];
  loading: boolean;
  timezoneName?: string;
}

export const ListingAnalyticsDetailTable: React.FC<ListingAnalyticsDetailTableProps> = ({
  metrics,
  loading,
  timezoneName = 'America/Sao_Paulo (UTC-03:00)',
}) => {
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const formatValue = (metric: AccountMetricDto) => {
    if (metric.metricValue === null || metric.metricValue === undefined) {
      return <span className="text-slate-400 italic">—</span>;
    }
    if (metric.metricUnit === 'CURRENCY') {
      return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(metric.metricValue);
    }
    if (metric.metricUnit === 'PERCENTAGE') {
      return `${Number(metric.metricValue).toFixed(2)}%`;
    }
    return new Intl.NumberFormat('pt-BR').format(metric.metricValue);
  };

  const renderStatusBadge = (status: MetricAvailabilityStatus) => {
    switch (status) {
      case 'AVAILABLE':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800">
            Disponível
          </span>
        );
      case 'UNSUPPORTED_BY_CHANNEL':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-semibold bg-purple-50 dark:bg-purple-950/60 text-purple-700 dark:text-purple-300 border border-purple-200 dark:border-purple-800">
            Não Suportado
          </span>
        );
      case 'INSUFFICIENT_DATA':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-semibold bg-sky-50 dark:bg-sky-950/60 text-sky-700 dark:text-sky-300 border border-sky-200 dark:border-sky-800">
            Dados Insuficientes
          </span>
        );
      case 'UNAVAILABLE':
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-semibold bg-amber-50 dark:bg-amber-950/60 text-amber-700 dark:text-amber-300 border border-amber-200 dark:border-amber-800">
            Indisponível
          </span>
        );
    }
  };

  const formatWindowDate = (isoStr: string) => {
    if (!isoStr) return '—';
    try {
      const d = new Date(isoStr);
      return d.toLocaleDateString('pt-BR', { timeZone: 'America/Sao_Paulo' });
    } catch {
      return isoStr.substring(0, 10);
    }
  };

  return (
    <div className="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
      {/* Cabeçalho da Tabela */}
      <div className="px-5 py-3.5 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between">
        <div>
          <h3 className="text-sm font-bold text-slate-900 dark:text-white">
            Registros Auditáveis de Métricas ({metrics.length})
          </h3>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            Origem comprovada, identificador do alvo e datas normalizadas sob {timezoneName}.
          </p>
        </div>
      </div>

      {loading ? (
        <div className="p-8 space-y-3 animate-pulse">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="h-10 bg-slate-100 dark:bg-slate-800 rounded" />
          ))}
        </div>
      ) : metrics.length === 0 ? (
        <div className="p-10 text-center text-xs text-slate-500">
          Nenhuma métrica persistida para o anúncio e filtros informados.
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-xs text-left">
            <thead className="bg-slate-50 dark:bg-slate-800/80 text-slate-700 dark:text-slate-300 font-semibold border-b border-slate-200 dark:border-slate-800 select-none">
              <tr>
                <th className="py-2.5 px-4">Métrica & Escopo</th>
                <th className="py-2.5 px-3">Alvo / Anúncio</th>
                <th className="py-2.5 px-3">Janela ({timezoneName.split(' ')[0]})</th>
                <th className="py-2.5 px-3">Valor</th>
                <th className="py-2.5 px-3">Status</th>
                <th className="py-2.5 px-3">Origem</th>
                <th className="py-2.5 px-4 text-right">Ação</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800 text-slate-800 dark:text-slate-200">
              {metrics.map((m) => {
                const isExpanded = expandedId === m.id;
                return (
                  <React.Fragment key={m.id}>
                    <tr className="hover:bg-slate-50/70 dark:hover:bg-slate-800/50 transition-colors">
                      <td className="py-2.5 px-4">
                        <div className="font-semibold text-slate-900 dark:text-white flex items-center gap-1.5">
                          <span>{m.metricKey}</span>
                          <span className="text-[10px] px-1.5 py-0.2 rounded bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400">
                            {m.metricScope}
                          </span>
                        </div>
                        <div className="text-[11px] text-slate-400 mt-0.5">
                          Granularidade: {m.granularity}
                        </div>
                      </td>

                      <td className="py-2.5 px-3 font-mono text-[11px]">
                        {m.externalListingId ? (
                          <span className="px-1.5 py-0.5 rounded bg-blue-50 dark:bg-blue-950/60 text-blue-700 dark:text-blue-300">
                            {m.externalListingId}
                          </span>
                        ) : (
                          <span className="text-slate-400 italic">Conta Inteira</span>
                        )}
                      </td>

                      <td className="py-2.5 px-3 font-mono text-[11px] whitespace-nowrap">
                        {formatWindowDate(m.windowStart)} → {formatWindowDate(m.windowEnd)}
                      </td>

                      <td className="py-2.5 px-3 font-semibold text-slate-900 dark:text-white">
                        {formatValue(m)}
                      </td>

                      <td className="py-2.5 px-3">
                        {renderStatusBadge(m.availabilityStatus)}
                      </td>

                      <td className="py-2.5 px-3 text-[11px]">
                        <span className="font-medium text-slate-700 dark:text-slate-300">
                          {m.provenance}
                        </span>
                        <div className="font-mono text-[10px] text-slate-400 truncate max-w-[180px]">
                          {m.sourceEndpoint}
                        </div>
                      </td>

                      <td className="py-2.5 px-4 text-right">
                        <button
                          type="button"
                          onClick={() => setExpandedId(isExpanded ? null : m.id)}
                          className="text-xs text-blue-600 dark:text-blue-400 hover:underline font-medium focus:outline-none min-h-[36px] py-1 px-2 rounded"
                        >
                          {isExpanded ? 'Ocultar' : 'Detalhes'}
                        </button>
                      </td>
                    </tr>

                    {/* Detalhes Expandidos da Linha */}
                    {isExpanded && (
                      <tr className="bg-slate-50 dark:bg-slate-800/40 border-b border-slate-200 dark:border-slate-800">
                        <td colSpan={7} className="p-4 space-y-2 text-xs">
                          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                            <div className="p-3 rounded-lg bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800">
                              <span className="font-bold text-slate-700 dark:text-slate-300 block mb-1">
                                Trilha de Cálculo & Fórmula:
                              </span>
                              {m.formulaDefinition ? (
                                <code className="block text-[11px] font-mono bg-slate-100 dark:bg-slate-800 p-2 rounded text-blue-600 dark:text-blue-400 break-all">
                                  {m.formulaDefinition}
                                </code>
                              ) : (
                                <p className="text-slate-500 italic">Métrica nativa oficial (sem fórmula derivada).</p>
                              )}
                              {m.denominatorMetric && (
                                <div className="mt-2 text-[11px] text-slate-600 dark:text-slate-400 space-y-0.5">
                                  <div>
                                    Denominador: <strong>{m.denominatorMetric}</strong> = {m.denominatorValue ?? '0'}
                                  </div>
                                  <div>
                                    Numerador: <strong>{m.numeratorMetric}</strong> = {m.numeratorValue ?? '0'}
                                  </div>
                                </div>
                              )}
                            </div>

                            <div className="p-3 rounded-lg bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800">
                              <span className="font-bold text-slate-700 dark:text-slate-300 block mb-1">
                                Auditoria & Disponibilidade:
                              </span>
                              <div className="space-y-1 text-[11px] text-slate-600 dark:text-slate-400">
                                <div>
                                  Endpoint Oficial: <code className="font-mono text-slate-800 dark:text-slate-200">{m.sourceEndpoint}</code>
                                </div>
                                <div>
                                  Janela UTC: <span className="font-mono">{m.windowStart}</span> até <span className="font-mono">{m.windowEnd}</span>
                                </div>
                                <div>
                                  Ingerido em: <span className="font-mono">{m.ingestedAt}</span>
                                </div>
                                {m.unavailabilityReason && (
                                  <div className="text-amber-700 dark:text-amber-400 pt-1 font-medium">
                                    Motivo de Indisponibilidade: {m.unavailabilityReason}
                                  </div>
                                )}
                              </div>
                            </div>
                          </div>
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};
