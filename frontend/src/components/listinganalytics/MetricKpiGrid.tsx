'use client';

import React, { useState } from 'react';
import { MetricsSummaryDto, MetricAvailabilityStatus } from '@/types/metrics';

export interface MetricKpiGridProps {
  summary: MetricsSummaryDto | null;
  previousSummary: MetricsSummaryDto | null;
  isComparing: boolean;
  marketplace: string;
  onOpenCatalog: () => void;
}

interface KpiCardConfig {
  id: string;
  title: string;
  value: number | null;
  type: 'count' | 'currency' | 'percentage';
  previousValue?: number | null;
  availabilityStatus: MetricAvailabilityStatus;
  unavailabilityReason?: string | null;
  formulaDefinition: string;
  antiHallucinationNote: string;
  sourceEndpoint: string;
}

export const MetricKpiGrid: React.FC<MetricKpiGridProps> = ({
  summary,
  previousSummary,
  isComparing,
  marketplace,
  onOpenCatalog,
}) => {
  const [activeTooltip, setActiveTooltip] = useState<string | null>(null);

  if (!summary) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 animate-pulse">
        {[...Array(6)].map((_, i) => (
          <div
            key={i}
            className="h-32 bg-slate-100 dark:bg-slate-800/60 rounded-xl border border-slate-200 dark:border-slate-800"
          />
        ))}
      </div>
    );
  }

  const isMeli = marketplace === 'MERCADO_LIVRE';

  // Regra Anti-Alucinação Estrita:
  // Se for Mercado Livre e tráfego orgânico, CTR não é suportado no canal oficial do vendedor.
  const ctrAvailability: MetricAvailabilityStatus = isMeli
    ? 'UNSUPPORTED_BY_CHANNEL'
    : summary.totalImpressions > 0
    ? 'AVAILABLE'
    : 'INSUFFICIENT_DATA';

  const ctrReason = isMeli
    ? 'O Mercado Livre não fornece dados de impressões em tráfego orgânico via API oficial. Dados disponíveis apenas sob campanhas ativas do Mercado Ads. Proibido inventar impressões.'
    : summary.totalImpressions === 0
    ? '0 impressões registradas no período filtrado para cálculo de CTR.'
    : null;

  const conversionAvailability: MetricAvailabilityStatus =
    summary.totalVisits > 0 ? 'AVAILABLE' : 'INSUFFICIENT_DATA';

  const conversionReason =
    summary.totalVisits === 0
      ? '0 visitas registradas no período. Divisão por zero prevenida deterministamente (status INSUFFICIENT_DATA).'
      : null;

  const kpis: KpiCardConfig[] = [
    {
      id: 'visits',
      title: 'Visitas Oficiais',
      value: summary.totalVisits,
      type: 'count',
      previousValue: previousSummary?.totalVisits ?? null,
      availabilityStatus: 'AVAILABLE',
      formulaDefinition: 'Contagem total de visitas/page views no anúncio ou conta',
      antiHallucinationNote: 'Obtido diretamente via endpoint oficial de visitas do marketplace.',
      sourceEndpoint: isMeli ? '/items/{id}/visits/time_window' : '/api/v2/product/get_item_performance',
    },
    {
      id: 'orders',
      title: 'Pedidos de Venda',
      value: summary.totalOrders,
      type: 'count',
      previousValue: previousSummary?.totalOrders ?? null,
      availabilityStatus: 'AVAILABLE',
      formulaDefinition: 'Contagem de pedidos fechados no intervalo',
      antiHallucinationNote: 'Registrado a partir dos pedidos oficiais confirmados.',
      sourceEndpoint: isMeli ? '/orders/search' : '/api/v2/order/get_order_list',
    },
    {
      id: 'units',
      title: 'Unidades Vendidas',
      value: summary.totalUnitsSold,
      type: 'count',
      previousValue: previousSummary?.totalUnitsSold ?? null,
      availabilityStatus: 'AVAILABLE',
      formulaDefinition: 'Soma da quantidade física vendida nos itens dos pedidos',
      antiHallucinationNote: 'Conferido com os itens das ordens de venda oficiais.',
      sourceEndpoint: isMeli ? '/orders/search (order_items.quantity)' : '/api/v2/order/get_order_detail',
    },
    {
      id: 'revenue',
      title: 'Receita Bruta Confirmada',
      value: summary.totalRevenue,
      type: 'currency',
      previousValue: previousSummary?.totalRevenue ?? null,
      availabilityStatus: 'AVAILABLE',
      formulaDefinition: 'Soma do valor bruto (preço unitário × unidades) em BRL',
      antiHallucinationNote: 'Total bruto confirmado em moeda oficial sem deduções fiscais aplicadas.',
      sourceEndpoint: isMeli ? '/orders/search (total_amount)' : '/api/v2/order/get_order_detail (escrow)',
    },
    {
      id: 'conversion',
      title: 'Taxa de Conversão (Pedidos)',
      value: conversionAvailability === 'AVAILABLE' ? summary.overallConversionRateOrders : null,
      type: 'percentage',
      previousValue:
        previousSummary && previousSummary.totalVisits > 0
          ? previousSummary.overallConversionRateOrders
          : null,
      availabilityStatus: conversionAvailability,
      unavailabilityReason: conversionReason,
      formulaDefinition: 'conversao_pedidos = (total_pedidos / total_visitas) × 100',
      antiHallucinationNote:
        'Cálculo determinístico em BigDecimal com escala 4 e arredondamento HALF_EVEN. Sem estimativa silenciosa.',
      sourceEndpoint: 'DerivedFormulaEngine',
    },
    {
      id: 'ctr',
      title: 'CTR (Taxa de Cliques)',
      value: ctrAvailability === 'AVAILABLE' ? summary.overallCtr : null,
      type: 'percentage',
      previousValue:
        previousSummary && ctrAvailability === 'AVAILABLE' && previousSummary.totalImpressions > 0
          ? previousSummary.overallCtr
          : null,
      availabilityStatus: ctrAvailability,
      unavailabilityReason: ctrReason,
      formulaDefinition: 'ctr = (cliques / impressoes) × 100',
      antiHallucinationNote:
        'No Mercado Livre, a API de catálogo/visitas NÃO expõe impressões orgânicas. CTR só é reportado se comprovado via Shopee OpenAPI Ads ou Mercado Ads.',
      sourceEndpoint: isMeli ? 'UNSUPPORTED_BY_CHANNEL (Mercado Livre Orgânico)' : '/api/v2/ads/get_performance',
    },
  ];

  // Função para formatação do valor
  const formatValue = (val: number | null, type: 'count' | 'currency' | 'percentage') => {
    if (val === null || val === undefined) return '—';
    if (type === 'currency') {
      return new Intl.NumberFormat('pt-BR', {
        style: 'currency',
        currency: 'BRL',
      }).format(val);
    }
    if (type === 'percentage') {
      return `${Number(val).toFixed(2)}%`;
    }
    return new Intl.NumberFormat('pt-BR').format(val);
  };

  // Variação percentual determinística
  const calculateDelta = (current: number | null, prev: number | null | undefined) => {
    if (current === null || prev === null || prev === undefined) {
      return { text: 'Sem base anterior', status: 'NO_PREVIOUS' as const };
    }
    if (prev === 0) {
      if (current === 0) return { text: '0.00%', status: 'EQUAL' as const };
      return { text: 'Nova métrica', status: 'UP' as const };
    }
    const diff = current - prev;
    const pct = (diff / prev) * 100;
    const prefix = pct > 0 ? '+' : '';
    const status = pct > 0 ? ('UP' as const) : pct < 0 ? ('DOWN' as const) : ('EQUAL' as const);
    return {
      text: `${prefix}${pct.toFixed(1)}%`,
      status,
      diffFormatted: formatValue(diff, 'count'),
    };
  };

  const renderStatusBadge = (status: MetricAvailabilityStatus, reason?: string | null) => {
    switch (status) {
      case 'AVAILABLE':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
            Oficial
          </span>
        );
      case 'UNSUPPORTED_BY_CHANNEL':
        return (
          <span
            title={reason || 'Não suportado no canal'}
            className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-purple-50 dark:bg-purple-950/60 text-purple-700 dark:text-purple-300 border border-purple-200 dark:border-purple-800"
          >
            <span className="w-1.5 h-1.5 rounded-full bg-purple-500" />
            Não Suportado
          </span>
        );
      case 'INSUFFICIENT_DATA':
        return (
          <span
            title={reason || 'Dados insuficientes'}
            className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-sky-50 dark:bg-sky-950/60 text-sky-700 dark:text-sky-300 border border-sky-200 dark:border-sky-800"
          >
            <span className="w-1.5 h-1.5 rounded-full bg-sky-500" />
            Dados Insuficientes
          </span>
        );
      case 'UNAVAILABLE':
      default:
        return (
          <span
            title={reason || 'Temporariamente indisponível'}
            className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-amber-50 dark:bg-amber-950/60 text-amber-700 dark:text-amber-300 border border-amber-200 dark:border-amber-800"
          >
            <span className="w-1.5 h-1.5 rounded-full bg-amber-500" />
            Indisponível
          </span>
        );
    }
  };

  return (
    <div className="space-y-4">
      {/* Grade de Cards de KPIs */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-3.5">
        {kpis.map((kpi) => {
          const delta = isComparing ? calculateDelta(kpi.value, kpi.previousValue) : null;
          const isUnavailable = kpi.availabilityStatus !== 'AVAILABLE';

          return (
            <div
              key={kpi.id}
              className={`relative bg-white dark:bg-slate-900 rounded-xl p-4 border transition-shadow shadow-sm flex flex-col justify-between ${
                isUnavailable
                  ? 'border-dashed border-amber-300 dark:border-amber-800/80 bg-amber-50/20 dark:bg-amber-950/10'
                  : 'border-slate-200 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700'
              }`}
            >
              {/* Header do Card: Título + Badge de Status */}
              <div>
                <div className="flex items-start justify-between gap-1 mb-1.5">
                  <span className="text-xs font-medium text-slate-600 dark:text-slate-400 leading-tight">
                    {kpi.title}
                  </span>
                  <button
                    type="button"
                    onClick={() => setActiveTooltip(activeTooltip === kpi.id ? null : kpi.id)}
                    aria-label={`Ver fórmula e auditoria de ${kpi.title}`}
                    className="p-1 -mr-1 -mt-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500 rounded min-w-[24px] min-h-[24px] flex items-center justify-center shrink-0"
                  >
                    <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                      />
                    </svg>
                  </button>
                </div>

                <div className="mb-2">
                  {renderStatusBadge(kpi.availabilityStatus, kpi.unavailabilityReason)}
                </div>

                {/* Valor Principal */}
                <div className="mt-1">
                  <div
                    className={`text-xl font-bold tracking-tight ${
                      isUnavailable
                        ? 'text-slate-400 dark:text-slate-500 italic'
                        : 'text-slate-900 dark:text-white'
                    }`}
                  >
                    {formatValue(kpi.value, kpi.type)}
                  </div>
                  {isUnavailable && kpi.unavailabilityReason && (
                    <p className="text-[11px] text-amber-700 dark:text-amber-400 mt-1 line-clamp-2 leading-relaxed">
                      {kpi.unavailabilityReason}
                    </p>
                  )}
                </div>
              </div>

              {/* Comparação Temporal */}
              {isComparing && (
                <div className="mt-3 pt-2.5 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between text-xs">
                  <span className="text-[11px] text-slate-500 dark:text-slate-400">vs. período anterior:</span>
                  {delta?.status === 'UP' ? (
                    <span className="inline-flex items-center gap-0.5 font-semibold text-emerald-600 dark:text-emerald-400">
                      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 15l7-7 7 7" />
                      </svg>
                      {delta.text}
                    </span>
                  ) : delta?.status === 'DOWN' ? (
                    <span className="inline-flex items-center gap-0.5 font-semibold text-rose-600 dark:text-rose-400">
                      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M19 9l-7 7-7-7" />
                      </svg>
                      {delta.text}
                    </span>
                  ) : delta?.status === 'EQUAL' ? (
                    <span className="text-slate-500 dark:text-slate-400 font-medium">{delta.text}</span>
                  ) : (
                    <span className="text-slate-400 italic text-[10px]">{delta?.text}</span>
                  )}
                </div>
              )}

              {/* Tooltip Acessível Inline */}
              {activeTooltip === kpi.id && (
                <div
                  role="tooltip"
                  className="absolute left-0 right-0 top-full mt-2 z-30 p-3.5 bg-slate-900 dark:bg-slate-800 text-white rounded-lg shadow-xl text-xs space-y-2 border border-slate-700 animate-fadeIn"
                >
                  <div className="flex items-center justify-between font-semibold border-b border-slate-700 pb-1.5">
                    <span>{kpi.title}</span>
                    <button
                      type="button"
                      onClick={() => setActiveTooltip(null)}
                      className="text-slate-400 hover:text-white p-0.5"
                      aria-label="Fechar ajuda"
                    >
                      ✕
                    </button>
                  </div>
                  <div>
                    <span className="text-slate-400 block text-[10px] uppercase font-bold tracking-wider">
                      Fórmula Oficial:
                    </span>
                    <code className="text-[11px] font-mono text-blue-300 block bg-slate-950/60 p-1 rounded mt-0.5 break-all">
                      {kpi.formulaDefinition}
                    </code>
                  </div>
                  <div>
                    <span className="text-slate-400 block text-[10px] uppercase font-bold tracking-wider">
                      Origem & Endpoint:
                    </span>
                    <span className="text-[11px] text-slate-300 font-mono break-all">{kpi.sourceEndpoint}</span>
                  </div>
                  <div className="pt-1 border-t border-slate-700/60 text-[11px] text-amber-300">
                    <strong>Guardrail Anti-Alucinação:</strong> {kpi.antiHallucinationNote}
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Botão de Transparência do Catálogo */}
      <div className="flex items-center justify-end">
        <button
          type="button"
          onClick={onOpenCatalog}
          className="inline-flex items-center gap-1.5 text-xs text-blue-600 dark:text-blue-400 hover:underline font-medium focus:outline-none min-h-[36px] py-1 px-2 rounded"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
            />
          </svg>
          Ver Matriz de Capacidades Oficiais & Quotas de API
        </button>
      </div>
    </div>
  );
};
