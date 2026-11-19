'use client';

import React, { useState } from 'react';
import { OrderFinancialFact, ReconcileSettlementCommand } from '@/types/financial';

interface ReconcileSettlementModalProps {
  fact: OrderFinancialFact;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (updatedFact: OrderFinancialFact) => void;
}

export const ReconcileSettlementModal: React.FC<ReconcileSettlementModalProps> = ({
  fact,
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [settlementExternalId, setSettlementExternalId] = useState(fact.settlementExternalId || '');
  const [settlementDate, setSettlementDate] = useState(
    fact.settlementDate ? fact.settlementDate.split('T')[0] : new Date().toISOString().split('T')[0]
  );
  const [confirmedMarketplaceFee, setConfirmedMarketplaceFee] = useState(
    fact.marketplaceFee ? fact.marketplaceFee.toString() : ''
  );
  const [confirmedFixedFee, setConfirmedFixedFee] = useState(
    fact.fixedFee ? fact.fixedFee.toString() : ''
  );
  const [confirmedShippingCost, setConfirmedShippingCost] = useState(
    fact.shippingCost ? fact.shippingCost.toString() : ''
  );
  const [payoutAmount, setPayoutAmount] = useState('');
  const [notes, setNotes] = useState(fact.settlementNotes || '');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  if (!isOpen) return null;

  const currentFee = fact.marketplaceFee || 0;
  const currentFixed = fact.fixedFee || 0;
  const currentShip = fact.shippingCost || 0;

  const enteredFee = parseFloat(confirmedMarketplaceFee) || 0;
  const enteredFixed = parseFloat(confirmedFixedFee) || 0;
  const enteredShip = parseFloat(confirmedShippingCost) || 0;

  const feeDiff = Math.abs(enteredFee - currentFee);
  const fixedDiff = Math.abs(enteredFixed - currentFixed);
  const shipDiff = Math.abs(enteredShip - currentShip);

  const isDivergent = feeDiff > 0.01 || fixedDiff > 0.01 || shipDiff > 0.01;

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!settlementExternalId.trim()) {
      setErrorMessage('Identificador do repasse/extrato é obrigatório.');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage(null);

    const payload: ReconcileSettlementCommand = {
      settlementExternalId: settlementExternalId.trim(),
      settlementDate: new Date(settlementDate).toISOString(),
      confirmedMarketplaceFee: parseFloat(confirmedMarketplaceFee) || 0,
      confirmedFixedFee: parseFloat(confirmedFixedFee) || 0,
      confirmedShippingCost: parseFloat(confirmedShippingCost) || 0,
      payoutAmount: payoutAmount ? parseFloat(payoutAmount) : null,
      notes: notes.trim() || null,
    };

    try {
      const res = await fetch(`/api/financial-facts/orders/${fact.orderId}/reconcile`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data.message || 'Falha ao conciliar pedido com extrato externo.');
      }

      const updated = await res.json();
      onSuccess(updated);
      onClose();
    } catch (err: unknown) {
      setErrorMessage(err instanceof Error ? err.message : 'Erro ao submeter conciliação.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-reconcile-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm"
      onKeyDown={(e) => {
        if (e.key === 'Escape') onClose();
      }}
    >
      <div className="w-full max-w-lg rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-2xl p-6 overflow-y-auto max-h-[90vh]">
        <div className="flex items-center justify-between border-b border-slate-100 dark:border-slate-800 pb-3">
          <h2 id="modal-reconcile-title" className="text-base font-bold text-slate-900 dark:text-white">
            Conciliar com Extrato Externo (Liquidação)
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Fechar modal"
            className="min-h-[44px] min-w-[44px] text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 text-lg flex items-center justify-center rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            ✕
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          {errorMessage && (
            <div role="alert" className="p-3 rounded-md bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900 text-xs text-rose-700 dark:text-rose-300">
              {errorMessage}
            </div>
          )}

          {/* Pedido referenciado */}
          <div className="p-3 rounded-lg bg-slate-50 dark:bg-slate-800/50 border border-slate-200 dark:border-slate-800 text-xs">
            <span className="text-slate-500">Pedido: </span>
            <span className="font-bold text-slate-900 dark:text-white">{fact.externalOrderId}</span>
            <span className="mx-2 text-slate-300">|</span>
            <span className="text-slate-500">Canal: </span>
            <span className="font-semibold text-slate-800 dark:text-slate-200">{fact.marketplace}</span>
          </div>

          {/* ID do Repasse / Extrato */}
          <div>
            <label htmlFor="input-settle-id" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              ID do Extrato / Repasse Externo *
            </label>
            <input
              id="input-settle-id"
              type="text"
              required
              placeholder="Ex: MP-SETTLE-889900 ou SHOPEE-ESCROW-1122"
              value={settlementExternalId}
              onChange={(e) => setSettlementExternalId(e.target.value)}
              className="min-h-[44px] w-full rounded-md border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 px-3 py-2 text-xs text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {/* Data do Repasse */}
          <div>
            <label htmlFor="input-settle-date" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Data da Liquidação / Repasse *
            </label>
            <input
              id="input-settle-date"
              type="date"
              required
              value={settlementDate}
              onChange={(e) => setSettlementDate(e.target.value)}
              className="min-h-[44px] w-full rounded-md border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 px-3 py-2 text-xs text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {/* Tarifas Confirmadas */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label htmlFor="input-confirm-fee" className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                Comissão Real (R$)
              </label>
              <input
                id="input-confirm-fee"
                type="number"
                step="0.0001"
                value={confirmedMarketplaceFee}
                onChange={(e) => setConfirmedMarketplaceFee(e.target.value)}
                className="min-h-[44px] w-full rounded-md border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 px-3 py-2 text-xs text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <span className="text-[10px] text-slate-400">Estimada: {formatCurrency(currentFee)}</span>
            </div>

            <div>
              <label htmlFor="input-confirm-fixed" className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                Tarifa Fixa Real (R$)
              </label>
              <input
                id="input-confirm-fixed"
                type="number"
                step="0.0001"
                value={confirmedFixedFee}
                onChange={(e) => setConfirmedFixedFee(e.target.value)}
                className="min-h-[44px] w-full rounded-md border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 px-3 py-2 text-xs text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <span className="text-[10px] text-slate-400">Estimada: {formatCurrency(currentFixed)}</span>
            </div>

            <div>
              <label htmlFor="input-confirm-ship" className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                Frete Real (R$)
              </label>
              <input
                id="input-confirm-ship"
                type="number"
                step="0.0001"
                value={confirmedShippingCost}
                onChange={(e) => setConfirmedShippingCost(e.target.value)}
                className="min-h-[44px] w-full rounded-md border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 px-3 py-2 text-xs text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <span className="text-[10px] text-slate-400">Estimado: {formatCurrency(currentShip)}</span>
            </div>
          </div>

          {/* Prévia da Conciliação */}
          <div className={`p-3 rounded-lg border text-xs ${
            isDivergent
              ? 'border-amber-300 bg-amber-50 dark:border-amber-900/60 dark:bg-amber-950/30 text-amber-900 dark:text-amber-200'
              : 'border-emerald-300 bg-emerald-50 dark:border-emerald-900/60 dark:bg-emerald-950/30 text-emerald-900 dark:text-emerald-200'
          }`}>
            <p className="font-bold flex items-center gap-1.5">
              <span>{isDivergent ? '⚠️ Status Resultante: DIVERGENTE' : '✓ Status Resultante: RECONCILIADO'}</span>
            </p>
            <p className="mt-0.5 text-[11px]">
              {isDivergent
                ? `Discrepância detectada entre estimativa e extrato (Comissão diff: ${formatCurrency(feeDiff)}, Frete diff: ${formatCurrency(shipDiff)}).`
                : 'Os valores do extrato batem exatamente com as regras estimadas (tolerância ≤ R$ 0,01).'}
            </p>
          </div>

          {/* Observações */}
          <div>
            <label htmlFor="input-settle-notes" className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
              Notas da Conciliação
            </label>
            <textarea
              id="input-settle-notes"
              rows={2}
              placeholder="Ex: Lançamento conferido no extrato bancário de 19/11..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="w-full rounded-md border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 px-3 py-2 text-xs text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {/* Botões */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100 dark:border-slate-800">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="min-h-[44px] px-4 py-2 text-xs font-semibold rounded-md text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="min-h-[44px] px-4 py-2 text-xs font-semibold rounded-md bg-blue-600 text-white hover:bg-blue-700 transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
            >
              {isSubmitting ? 'Salvando...' : 'Confirmar Reconciliação'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
