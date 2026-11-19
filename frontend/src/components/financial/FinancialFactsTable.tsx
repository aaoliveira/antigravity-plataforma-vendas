'use client';

import React from 'react';
import { OrderFinancialFact } from '@/types/financial';

interface FinancialFactsTableProps {
  facts: OrderFinancialFact[];
  isLoading: boolean;
  onSelectFact: (fact: OrderFinancialFact) => void;
  onRefreshFact?: (orderId: string) => void;
}

export const FinancialFactsTable: React.FC<FinancialFactsTableProps> = ({
  facts,
  isLoading,
  onSelectFact,
  onRefreshFact,
}) => {
  const formatCurrency = (val?: number) => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0);
  };

  const formatDate = (isoStr?: string) => {
    if (!isoStr) return '-';
    try {
      const d = new Date(isoStr);
      return d.toLocaleDateString('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return isoStr;
    }
  };

  if (isLoading) {
    return (
      <div className="rounded-lg border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-8 text-center">
        <div className="inline-block h-8 w-8 animate-spin rounded-full border-4 border-solid border-blue-600 border-r-transparent align-[-0.125em]" />
        <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">Carregando fatos financeiros...</p>
      </div>
    );
  }

  if (facts.length === 0) {
    return (
      <div className="rounded-lg border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-12 text-center">
        <p className="text-base font-semibold text-slate-800 dark:text-slate-200">
          Nenhum pedido encontrado para os filtros selecionados
        </p>
        <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
          Tente alterar o período, limpar os filtros ou selecionar outro canal/empresa.
        </p>
      </div>
    );
  }

  return (
    <div className="rounded-lg border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs text-slate-600 dark:text-slate-300" aria-label="Tabela de Fatos Financeiros por Pedido">
          <thead className="bg-slate-50 dark:bg-slate-800/60 text-[11px] uppercase tracking-wider text-slate-500 dark:text-slate-400 border-b border-slate-200 dark:border-slate-800">
            <tr>
              <th scope="col" className="px-4 py-3 font-semibold">Pedido / Data</th>
              <th scope="col" className="px-4 py-3 font-semibold">Canal / Status</th>
              <th scope="col" className="px-4 py-3 font-semibold text-right">Receita Líquida</th>
              <th scope="col" className="px-4 py-3 font-semibold text-right">Taxas + Frete</th>
              <th scope="col" className="px-4 py-3 font-semibold text-right">CMV / Custos</th>
              <th scope="col" className="px-4 py-3 font-semibold text-right">Margem Contrib.</th>
              <th scope="col" className="px-4 py-3 font-semibold text-center">Reconciliação</th>
              <th scope="col" className="px-4 py-3 font-semibold text-center">Ações</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
            {facts.map((fact) => {
              const channelAndShipping = (fact.marketplaceFee || 0) + (fact.fixedFee || 0) + (fact.shippingCost || 0);
              const directCosts = (fact.productCost || 0) + (fact.packagingCost || 0) + (fact.additionalCost || 0);
              const isPositiveMargin = (fact.contributionMarginAmount || 0) >= 0;

              return (
                <tr
                  key={fact.id}
                  className="hover:bg-slate-50/80 dark:hover:bg-slate-800/40 transition-colors"
                >
                  {/* Pedido / Data */}
                  <td className="px-4 py-3 whitespace-nowrap">
                    <div className="font-semibold text-slate-900 dark:text-white">
                      {fact.externalOrderId}
                    </div>
                    <div className="text-[11px] text-slate-500">
                      {formatDate(fact.orderDate)}
                    </div>
                  </td>

                  {/* Canal / Status do Pedido */}
                  <td className="px-4 py-3 whitespace-nowrap">
                    <div className="flex items-center gap-1.5">
                      <span className="font-medium text-slate-800 dark:text-slate-200">
                        {fact.marketplace === 'MERCADO_LIVRE' ? 'Mercado Livre' : fact.marketplace}
                      </span>
                    </div>
                    <div className="text-[10px] text-slate-500">
                      Status: {fact.orderStatus}
                    </div>
                  </td>

                  {/* Receita Líquida */}
                  <td className="px-4 py-3 whitespace-nowrap text-right font-medium text-slate-900 dark:text-white">
                    {formatCurrency(fact.netRevenue)}
                  </td>

                  {/* Taxas + Frete */}
                  <td className="px-4 py-3 whitespace-nowrap text-right text-rose-600 dark:text-rose-400">
                    - {formatCurrency(channelAndShipping)}
                  </td>

                  {/* CMV / Custos Diretos */}
                  <td className="px-4 py-3 whitespace-nowrap text-right text-amber-600 dark:text-amber-400">
                    - {formatCurrency(directCosts)}
                  </td>

                  {/* Margem de Contribuição */}
                  <td className="px-4 py-3 whitespace-nowrap text-right">
                    <div className={`font-bold ${isPositiveMargin ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400'}`}>
                      {formatCurrency(fact.contributionMarginAmount)}
                    </div>
                    <div className="text-[10px] text-slate-500">
                      {(fact.contributionMarginRatio || 0).toFixed(2)}%
                    </div>
                  </td>

                  {/* Reconciliação & Incompletude */}
                  <td className="px-4 py-3 whitespace-nowrap text-center">
                    <div className="flex flex-col items-center gap-1">
                      {fact.reconciliationStatus === 'RECONCILED' ? (
                        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300">
                          Reconciliado
                        </span>
                      ) : fact.reconciliationStatus === 'DIVERGENT' ? (
                        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-rose-100 text-rose-800 dark:bg-rose-950/60 dark:text-rose-300">
                          Divergente
                        </span>
                      ) : (
                        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-blue-100 text-blue-800 dark:bg-blue-950/60 dark:text-blue-300">
                          Estimado
                        </span>
                      )}

                      {fact.hasMissingData && (
                        <span
                          title={fact.missingDataReasons || 'Custos ou taxas não cadastrados'}
                          className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300 cursor-help"
                        >
                          ⚠️ Dados Incompletos
                        </span>
                      )}
                    </div>
                  </td>

                  {/* Ações */}
                  <td className="px-4 py-3 whitespace-nowrap text-center">
                    <button
                      type="button"
                      onClick={() => onSelectFact(fact)}
                      className="min-h-[44px] px-3 py-1.5 rounded-md text-xs font-semibold text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-950/30 transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                      Ver Detalhes
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};
