'use client';

import React from 'react';
import { AggregatedKpiSummary } from '@/types/kpis';

interface DataQualityAlertsProps {
  summary: AggregatedKpiSummary | null;
  isLoading: boolean;
}

export const DataQualityAlerts: React.FC<DataQualityAlertsProps> = ({
  summary,
  isLoading,
}) => {
  if (isLoading || !summary) return null;

  const hasMissing = summary.ordersWithMissingDataCount > 0;
  const hasDivergent = summary.divergentCount > 0;
  const hasEstimated = summary.estimatedCount > 0;
  const isAllReconciled = summary.reconciliationRatePercent === 100;

  return (
    <section aria-label="Alertas de Qualidade dos Dados" className="space-y-3">
      {/* Alerta de Dados Faltantes */}
      {hasMissing && (
        <div
          role="alert"
          className="p-3.5 rounded-xl border border-amber-300 dark:border-amber-700/60 bg-amber-50 dark:bg-amber-950/30 flex items-start gap-3 text-xs"
        >
          <svg className="w-5 h-5 text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <div className="space-y-1">
            <h4 className="font-semibold text-amber-900 dark:text-amber-200">
              {summary.ordersWithMissingDataCount} {summary.ordersWithMissingDataCount === 1 ? 'pedido contém' : 'pedidos contêm'} dados ausentes ou incompletos
            </h4>
            <p className="text-amber-800 dark:text-amber-300 text-[11px]">
              Custos unitários de mercadoria (CMV), alíquotas fiscais ou parâmetros de frete não foram informados para alguns itens. Estes valores foram mantidos como zero para evitar estimativas silenciosas não comprovadas.
            </p>
          </div>
        </div>
      )}

      {/* Alerta de Divergência Contábil */}
      {hasDivergent && (
        <div
          role="alert"
          className="p-3.5 rounded-xl border border-rose-300 dark:border-rose-700/60 bg-rose-50 dark:bg-rose-950/30 flex items-start gap-3 text-xs"
        >
          <svg className="w-5 h-5 text-rose-600 dark:text-rose-400 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 018 0z" />
          </svg>
          <div className="space-y-1">
            <h4 className="font-semibold text-rose-900 dark:text-rose-200">
              Divergência detectada em {summary.divergentCount} {summary.divergentCount === 1 ? 'pedido' : 'pedidos'}
            </h4>
            <p className="text-rose-800 dark:text-rose-300 text-[11px]">
              Os valores efetivamente debitados no extrato financeiro do marketplace diferem dos valores estimados inicialmente pela tabela de comissão/frete.
            </p>
          </div>
        </div>
      )}

      {/* Alerta Informativo de Conciliação Pendente */}
      {!hasMissing && !hasDivergent && hasEstimated && (
        <div
          role="status"
          className="p-3.5 rounded-xl border border-blue-200 dark:border-blue-800/60 bg-blue-50/70 dark:bg-blue-950/20 flex items-start gap-3 text-xs"
        >
          <svg className="w-5 h-5 text-blue-600 dark:text-blue-400 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 018 0z" />
          </svg>
          <div className="space-y-1">
            <h4 className="font-semibold text-blue-900 dark:text-blue-200">
              Conciliação em andamento ({summary.estimatedCount} pedidos com liquidação pendente)
            </h4>
            <p className="text-blue-800 dark:text-blue-300 text-[11px]">
              As comissões e fretes destes pedidos estão calculados com base nas tabelas vigentes de cada canal e serão confirmados após a importação do extrato de liquidação externa.
            </p>
          </div>
        </div>
      )}

      {/* Sucesso 100% Conciliado */}
      {isAllReconciled && !hasMissing && (
        <div
          role="status"
          className="p-3.5 rounded-xl border border-emerald-200 dark:border-emerald-800/60 bg-emerald-50/70 dark:bg-emerald-950/20 flex items-start gap-3 text-xs"
        >
          <svg className="w-5 h-5 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 018 0z" />
          </svg>
          <div>
            <h4 className="font-semibold text-emerald-900 dark:text-emerald-200">
              Dados 100% Reconciliados e Auditados
            </h4>
            <p className="text-emerald-800 dark:text-emerald-300 text-[11px]">
              Todos os {summary.totalOrders} pedidos do período foram formalmente liquidados e conferidos contra extratos dos canais.
            </p>
          </div>
        </div>
      )}
    </section>
  );
};
