'use client';

import React, { useState, useEffect } from 'react';
import { OrderFinancialFact } from '@/types/financial';
import { ReconcileSettlementModal } from './ReconcileSettlementModal';

interface FinancialFactDetailDrawerProps {
  fact: OrderFinancialFact | null;
  isOpen: boolean;
  onClose: () => void;
  onFactUpdated: (updated: OrderFinancialFact) => void;
}

export const FinancialFactDetailDrawer: React.FC<FinancialFactDetailDrawerProps> = ({
  fact,
  isOpen,
  onClose,
  onFactUpdated,
}) => {
  const [isReconcileModalOpen, setIsReconcileModalOpen] = useState(false);
  const [isRecomputing, setIsRecomputing] = useState(false);
  const [recomputeMessage, setRecomputeMessage] = useState<string | null>(null);
  const [isTraceExpanded, setIsTraceExpanded] = useState(false);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen && !isReconcileModalOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, isReconcileModalOpen, onClose]);

  if (!isOpen || !fact) return null;

  const formatCurrency = (val?: number) => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0);
  };

  const formatDate = (isoStr?: string) => {
    if (!isoStr) return '-';
    try {
      return new Date(isoStr).toLocaleDateString('pt-BR', {
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

  const handleRecompute = async () => {
    setIsRecomputing(true);
    setRecomputeMessage(null);
    try {
      const res = await fetch(`/api/financial-facts/orders/${fact.orderId}/compute`, {
        method: 'POST',
      });
      if (!res.ok) throw new Error('Falha ao reprocessar fatos financeiros.');
      const data = await res.json();
      onFactUpdated(data);
      setRecomputeMessage('Fatos recalculados com sucesso!');
    } catch (err: unknown) {
      setRecomputeMessage(err instanceof Error ? err.message : 'Erro ao recalcular fatos.');
    } finally {
      setIsRecomputing(false);
    }
  };

  const totalDeductionsCalc = fact.totalDeductions || 0;
  const isPositiveMargin = (fact.contributionMarginAmount || 0) >= 0;

  return (
    <>
      <div
        role="dialog"
        aria-modal="true"
        aria-label={`Detalhamento Financeiro do Pedido ${fact.externalOrderId}`}
        className="fixed inset-0 z-40 flex justify-end bg-black/40 backdrop-blur-sm"
      >
        <div className="w-full max-w-2xl bg-white dark:bg-slate-900 border-l border-slate-200 dark:border-slate-800 shadow-2xl flex flex-col h-full overflow-hidden">
          {/* Cabeçalho */}
          <div className="p-4 sm:p-5 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between bg-slate-50/50 dark:bg-slate-850">
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base font-bold text-slate-900 dark:text-white">
                  Pedido {fact.externalOrderId}
                </h2>
                {fact.reconciliationStatus === 'RECONCILED' ? (
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300">
                    Reconciliado
                  </span>
                ) : fact.reconciliationStatus === 'DIVERGENT' ? (
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-100 text-rose-800 dark:bg-rose-950 dark:text-rose-300">
                    Divergente
                  </span>
                ) : (
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300">
                    Estimado
                  </span>
                )}
              </div>
              <p className="text-xs text-slate-500 mt-0.5">
                Canal: <strong className="text-slate-700 dark:text-slate-300">{fact.marketplace}</strong> &bull; Data: {formatDate(fact.orderDate)}
              </p>
            </div>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={handleRecompute}
                disabled={isRecomputing}
                className="min-h-[44px] px-3 py-1.5 text-xs font-semibold rounded-md bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                {isRecomputing ? 'Recalculando...' : 'Recalcular'}
              </button>

              <button
                type="button"
                onClick={() => setIsReconcileModalOpen(true)}
                className="min-h-[44px] px-3 py-1.5 text-xs font-semibold rounded-md bg-blue-600 text-white hover:bg-blue-700 transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                Conciliar Extrato
              </button>

              <button
                type="button"
                onClick={onClose}
                aria-label="Fechar painel de detalhes"
                className="min-h-[44px] min-w-[44px] text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 text-lg flex items-center justify-center rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                ✕
              </button>
            </div>
          </div>

          {/* Feedback de Recálculo */}
          {recomputeMessage && (
            <div role="status" className="p-3 bg-blue-50 dark:bg-blue-950/40 text-blue-800 dark:text-blue-300 text-xs border-b border-blue-200 dark:border-blue-900">
              {recomputeMessage}
            </div>
          )}

          {/* Conteúdo com Scroll */}
          <div className="flex-1 overflow-y-auto p-4 sm:p-5 space-y-6">
            {/* Alerta de Incompletude */}
            {fact.hasMissingData && (
              <div role="alert" className="p-3.5 rounded-lg bg-amber-50 dark:bg-amber-950/30 border border-amber-300 dark:border-amber-900/60 text-xs text-amber-900 dark:text-amber-200 space-y-1">
                <p className="font-bold flex items-center gap-1.5">
                  <span>⚠️</span> Este pedido possui dados ausentes ou incompletos:
                </p>
                <p className="text-[11px] font-mono text-amber-800 dark:text-amber-300">
                  Motivo: {fact.missingDataReasons || 'Custo de aquisição ou tabela de comissão não cadastrada.'}
                </p>
                <p className="text-[11px] text-amber-700 dark:text-amber-400">
                  Para obter a margem real completa, cadastre os custos do produto no menu Finanças / Custos de Produtos.
                </p>
              </div>
            )}

            {/* DRE Gerencial Cascata */}
            <div className="rounded-lg border border-slate-200 dark:border-slate-800 p-4 bg-white dark:bg-slate-900">
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-3">
                DRE Gerencial Simplificada (Visão Pedido)
              </h3>

              <div className="space-y-2 text-xs">
                {/* (+) Receita Bruta */}
                <div className="flex justify-between py-1 border-b border-slate-100 dark:border-slate-800">
                  <span className="font-medium text-slate-700 dark:text-slate-300">(+) Receita Bruta</span>
                  <span className="font-semibold text-slate-900 dark:text-white">{formatCurrency(fact.grossRevenue)}</span>
                </div>

                {/* (-) Descontos */}
                {(fact.discountAmount || 0) > 0 && (
                  <div className="flex justify-between py-1 text-rose-600 dark:text-rose-400 border-b border-slate-100 dark:border-slate-800">
                    <span>(-) Descontos Concedidos</span>
                    <span>- {formatCurrency(fact.discountAmount)}</span>
                  </div>
                )}

                {/* (=) Receita Líquida */}
                <div className="flex justify-between py-1 font-bold text-slate-900 dark:text-white bg-slate-50 dark:bg-slate-800/40 px-2 rounded">
                  <span>(=) Receita Líquida de Vendas</span>
                  <span>{formatCurrency(fact.netRevenue)}</span>
                </div>

                {/* Deduções de Venda */}
                <div className="pt-2 pl-2 space-y-1 text-slate-600 dark:text-slate-400">
                  <div className="flex justify-between py-0.5">
                    <span>(-) Comissão do Marketplace</span>
                    <span className="text-rose-600 dark:text-rose-400">- {formatCurrency(fact.marketplaceFee)}</span>
                  </div>
                  <div className="flex justify-between py-0.5">
                    <span>(-) Tarifa Fixa de Transação</span>
                    <span className="text-rose-600 dark:text-rose-400">- {formatCurrency(fact.fixedFee)}</span>
                  </div>
                  <div className="flex justify-between py-0.5">
                    <span>(-) Frete Seller (Subsidiado)</span>
                    <span className="text-rose-600 dark:text-rose-400">- {formatCurrency(fact.shippingCost)}</span>
                  </div>
                  <div className="flex justify-between py-0.5">
                    <span>(-) Custo de Aquisição (CMV)</span>
                    <span className="text-amber-600 dark:text-amber-400">- {formatCurrency(fact.productCost)}</span>
                  </div>
                  <div className="flex justify-between py-0.5">
                    <span>(-) Embalagens & Insumos</span>
                    <span className="text-amber-600 dark:text-amber-400">- {formatCurrency(fact.packagingCost)}</span>
                  </div>
                  <div className="flex justify-between py-0.5">
                    <span>(-) Impostos Informados</span>
                    <span className="text-indigo-600 dark:text-indigo-400">- {formatCurrency(fact.taxAmount)}</span>
                  </div>
                  {(fact.otherExpensesAmount || 0) > 0 && (
                    <div className="flex justify-between py-0.5">
                      <span>(-) Outras Despesas Variáveis</span>
                      <span className="text-rose-600 dark:text-rose-400">- {formatCurrency(fact.otherExpensesAmount)}</span>
                    </div>
                  )}
                </div>

                {/* Total Deduções */}
                <div className="flex justify-between py-1 font-semibold text-rose-600 dark:text-rose-400 border-t border-slate-100 dark:border-slate-800">
                  <span>Total de Deduções Variáveis</span>
                  <span>- {formatCurrency(totalDeductionsCalc)}</span>
                </div>

                {/* (=) Margem de Contribuição */}
                <div className={`flex justify-between items-center py-2 px-3 rounded-lg border-2 ${
                  isPositiveMargin
                    ? 'border-emerald-500/40 bg-emerald-50/60 dark:bg-emerald-950/30 text-emerald-900 dark:text-emerald-200'
                    : 'border-rose-500/40 bg-rose-50/60 dark:bg-rose-950/30 text-rose-900 dark:text-rose-200'
                }`}>
                  <div>
                    <span className="font-bold text-sm block">(=) Margem de Contribuição</span>
                    <span className="text-[10px] text-slate-500">
                      Sobra para cobertura de custos fixos da empresa
                    </span>
                  </div>
                  <div className="text-right">
                    <span className="text-base font-extrabold block">
                      {formatCurrency(fact.contributionMarginAmount)}
                    </span>
                    <span className="text-xs font-bold">
                      {(fact.contributionMarginRatio || 0).toFixed(2)}%
                    </span>
                  </div>
                </div>

                {/* Nota Anti-Alucinação */}
                <div className="p-2 rounded bg-amber-50/80 dark:bg-amber-950/20 text-[10px] text-amber-900 dark:text-amber-300">
                  ⚖️ <strong>Regra de Integridade Financeira:</strong> Este demonstrativo apura estritamente a Margem de Contribuição. É expressamente proibido rotular este valor como Lucro Líquido sem a modelagem integral dos custos fixos estruturais.
                </div>
              </div>
            </div>

            {/* Detalhamento dos Itens do Pedido */}
            <div className="rounded-lg border border-slate-200 dark:border-slate-800 p-4 bg-white dark:bg-slate-900">
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-3">
                Composição Granular por Item ({fact.items?.length || 0})
              </h3>

              <div className="space-y-3">
                {fact.items?.map((item) => (
                  <div
                    key={item.id}
                    className="p-3 rounded-md border border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-850/50 text-xs space-y-2"
                  >
                    <div className="flex justify-between items-start">
                      <div>
                        <p className="font-semibold text-slate-900 dark:text-white">{item.title}</p>
                        <p className="text-[11px] text-slate-500 font-mono">
                          SKU: {item.sku || 'N/A'} &bull; Qtd: {item.quantity} × {formatCurrency(item.unitPrice)}
                        </p>
                      </div>
                      <div className="text-right">
                        <span className="font-bold text-slate-900 dark:text-white">
                          {formatCurrency(item.grossRevenue)}
                        </span>
                        {item.hasMissingData && (
                          <span className="block text-[10px] text-rose-500 font-semibold">
                            ⚠️ Custo Ausente
                          </span>
                        )}
                      </div>
                    </div>

                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-2 border-t border-slate-200/60 dark:border-slate-700/60 text-[11px]">
                      <div>
                        <span className="text-slate-400 block text-[10px]">CMV Total:</span>
                        <span className="font-medium text-slate-700 dark:text-slate-300">
                          {formatCurrency(item.productCost)}
                        </span>
                      </div>
                      <div>
                        <span className="text-slate-400 block text-[10px]">Comissão:</span>
                        <span className="font-medium text-slate-700 dark:text-slate-300">
                          {formatCurrency(item.marketplaceFee)}
                        </span>
                      </div>
                      <div>
                        <span className="text-slate-400 block text-[10px]">Frete Rateado:</span>
                        <span className="font-medium text-slate-700 dark:text-slate-300">
                          {formatCurrency(item.shippingCost)}
                        </span>
                      </div>
                      <div>
                        <span className="text-slate-400 block text-[10px]">Margem Item:</span>
                        <span className={`font-bold ${item.contributionMarginAmount >= 0 ? 'text-emerald-600' : 'text-rose-600'}`}>
                          {formatCurrency(item.contributionMarginAmount)} ({(item.contributionMarginRatio || 0).toFixed(1)}%)
                        </span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Reconciliação com Extrato Externo */}
            {fact.settlementExternalId && (
              <div className="rounded-lg border border-slate-200 dark:border-slate-800 p-4 bg-slate-50 dark:bg-slate-850 text-xs space-y-2">
                <h3 className="font-bold text-slate-900 dark:text-white flex items-center justify-between">
                  <span>Reconciliação Externa Registrada</span>
                  <span className="font-mono text-[11px] px-2 py-0.5 rounded bg-slate-200 dark:bg-slate-700">
                    {fact.settlementExternalId}
                  </span>
                </h3>
                <p className="text-slate-600 dark:text-slate-300 text-[11px]">
                  Data do Repasse: <strong>{formatDate(fact.settlementDate || '')}</strong> &bull; Reconciliado em: {formatDate(fact.reconciledAt || '')}
                </p>
                {fact.settlementNotes && (
                  <p className="text-[11px] text-slate-500 italic">
                    Notas: &quot;{fact.settlementNotes}&quot;
                  </p>
                )}
              </div>
            )}

            {/* Trilha de Cálculo e Auditoria */}
            <div className="rounded-lg border border-slate-200 dark:border-slate-800 p-3 bg-white dark:bg-slate-900 text-xs">
              <button
                type="button"
                onClick={() => setIsTraceExpanded(!isTraceExpanded)}
                className="w-full flex items-center justify-between font-semibold text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white"
              >
                <span>Trilha de Cálculo Auditável (Versão {fact.calculationVersion})</span>
                <span>{isTraceExpanded ? '▲ Ocultar' : '▼ Expandir'}</span>
              </button>

              {isTraceExpanded && fact.calculationTraceJson && (
                <pre className="mt-3 p-3 rounded bg-slate-950 text-slate-200 font-mono text-[10px] overflow-x-auto max-h-60">
                  {(() => {
                    try {
                      return JSON.stringify(JSON.parse(fact.calculationTraceJson), null, 2);
                    } catch {
                      return fact.calculationTraceJson;
                    }
                  })()}
                </pre>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Modal de Reconciliação */}
      <ReconcileSettlementModal
        fact={fact}
        isOpen={isReconcileModalOpen}
        onClose={() => setIsReconcileModalOpen(false)}
        onSuccess={(updated) => {
          onFactUpdated(updated);
        }}
      />
    </>
  );
};
