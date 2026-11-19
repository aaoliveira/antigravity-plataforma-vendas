'use client';

import React, { useState, useEffect } from 'react';
import { ProductCost } from '@/types/pricing';

interface ProductCostFormProps {
  initialProductId?: string;
  onCostSaved?: (cost: ProductCost) => void;
}

interface CostFormState {
  productId: string;
  variantId: string;
  companyId: string;
  currency: string;
  unit: string;
  acquisitionCost: string;
  packagingCost: string;
  additionalCost: string;
  subsidizedShippingCost: string;
  taxRatePercent: string;
  otherExpensesPercent: string;
  effectiveFrom: string;
  sourceType: string;
  notes: string;
}

interface CostFormErrors {
  productId?: string;
  acquisitionCost?: string;
  packagingCost?: string;
  additionalCost?: string;
  subsidizedShippingCost?: string;
  taxRatePercent?: string;
  otherExpensesPercent?: string;
  effectiveFrom?: string;
}

export function ProductCostForm({ initialProductId, onCostSaved }: ProductCostFormProps) {
  const [formState, setFormState] = useState<CostFormState>({
    productId: initialProductId || '',
    variantId: '',
    companyId: '',
    currency: 'BRL',
    unit: 'UN',
    acquisitionCost: '',
    packagingCost: '0.00',
    additionalCost: '0.00',
    subsidizedShippingCost: '0.00',
    taxRatePercent: '0.00',
    otherExpensesPercent: '0.00',
    effectiveFrom: new Date().toISOString().slice(0, 16),
    sourceType: 'USER_INPUT',
    notes: '',
  });

  const [errors, setErrors] = useState<CostFormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitFeedback, setSubmitFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  // Histórico
  const [history, setHistory] = useState<ProductCost[]>([]);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);

  // Validação UUID simples
  const isValidUuid = (value: string) => {
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
  };

  const validate = (): boolean => {
    const newErrors: CostFormErrors = {};

    if (!formState.productId.trim()) {
      newErrors.productId = 'O ID do produto é obrigatório.';
    } else if (!isValidUuid(formState.productId.trim())) {
      newErrors.productId = 'Formato de ID inválido. Deve ser um UUID válido.';
    }

    const acqCost = parseFloat(formState.acquisitionCost);
    if (!formState.acquisitionCost.trim() || isNaN(acqCost)) {
      newErrors.acquisitionCost = 'Custo de aquisição é obrigatório e deve ser numérico.';
    } else if (acqCost <= 0) {
      newErrors.acquisitionCost = 'Custo de aquisição deve ser maior que zero.';
    }

    const packCost = parseFloat(formState.packagingCost);
    if (formState.packagingCost.trim() && (isNaN(packCost) || packCost < 0)) {
      newErrors.packagingCost = 'Custo de embalagem não pode ser negativo.';
    }

    const addCost = parseFloat(formState.additionalCost);
    if (formState.additionalCost.trim() && (isNaN(addCost) || addCost < 0)) {
      newErrors.additionalCost = 'Custo adicional não pode ser negativo.';
    }

    const shipCost = parseFloat(formState.subsidizedShippingCost);
    if (formState.subsidizedShippingCost.trim() && (isNaN(shipCost) || shipCost < 0)) {
      newErrors.subsidizedShippingCost = 'Frete subsidiado não pode ser negativo.';
    }

    const tax = parseFloat(formState.taxRatePercent);
    if (formState.taxRatePercent.trim() && (isNaN(tax) || tax < 0 || tax > 100)) {
      newErrors.taxRatePercent = 'Alíquota de imposto deve estar entre 0% e 100%.';
    }

    const otherExp = parseFloat(formState.otherExpensesPercent);
    if (formState.otherExpensesPercent.trim() && (isNaN(otherExp) || otherExp < 0 || otherExp > 100)) {
      newErrors.otherExpensesPercent = 'Outras despesas devem estar entre 0% e 100%.';
    }

    if (!formState.effectiveFrom.trim()) {
      newErrors.effectiveFrom = 'Data/hora de vigência inicial é obrigatória.';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const loadHistory = async (prodId: string) => {
    if (!prodId.trim() || !isValidUuid(prodId.trim())) {
      setHistory([]);
      return;
    }
    setIsLoadingHistory(true);
    try {
      const res = await fetch(`/api/pricing/costs/history?productId=${encodeURIComponent(prodId.trim())}`);
      if (res.ok) {
        const data = await res.json();
        setHistory(data);
      } else {
        setHistory([]);
      }
    } catch {
      setHistory([]);
    } finally {
      setIsLoadingHistory(false);
    }
  };

  useEffect(() => {
    if (formState.productId.trim() && isValidUuid(formState.productId.trim())) {
      loadHistory(formState.productId.trim());
    }
  }, [formState.productId]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitFeedback(null);

    if (!validate()) {
      return;
    }

    setIsSubmitting(true);
    try {
      const effectiveIso = new Date(formState.effectiveFrom).toISOString();
      const payload = {
        productId: formState.productId.trim(),
        variantId: formState.variantId.trim() ? formState.variantId.trim() : null,
        companyId: formState.companyId.trim() ? formState.companyId.trim() : null,
        currency: formState.currency,
        unit: formState.unit,
        acquisitionCost: parseFloat(formState.acquisitionCost),
        packagingCost: parseFloat(formState.packagingCost) || 0,
        additionalCost: parseFloat(formState.additionalCost) || 0,
        subsidizedShippingCost: parseFloat(formState.subsidizedShippingCost) || 0,
        taxRatePercent: parseFloat(formState.taxRatePercent) || 0,
        otherExpensesPercent: parseFloat(formState.otherExpensesPercent) || 0,
        effectiveFrom: effectiveIso,
        sourceType: formState.sourceType,
        notes: formState.notes.trim() || null,
      };

      const res = await fetch('/api/pricing/costs', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Erro ao registrar versão de custo.');
      }

      const created: ProductCost = await res.json();
      setSubmitFeedback({
        type: 'success',
        message: `Custo registrado com sucesso! Versão ${created.version} já vigente no tenant.`,
      });

      if (onCostSaved) {
        onCostSaved(created);
      }

      // Recarrega histórico
      loadHistory(created.productId);
    } catch (err: any) {
      setSubmitFeedback({
        type: 'error',
        message: err.message || 'Falha ao salvar custo do produto.',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-8">
      {/* Formulário de Cadastro */}
      <section
        aria-labelledby="cost-form-heading"
        className="bg-white dark:bg-slate-900 rounded-xl p-6 border border-slate-200 dark:border-slate-800 shadow-sm"
      >
        <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800">
          <div>
            <h2 id="cost-form-heading" className="text-base font-bold text-slate-900 dark:text-white">
              Cadastro e Versionamento de Custos
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              Defina os custos diretos e alíquotas operacionais. Atualizações geram nova versão sem apagar o histórico pretérito.
            </p>
          </div>
          <span className="text-xs px-2.5 py-1 bg-blue-50 dark:bg-blue-950/40 text-blue-700 dark:text-blue-300 rounded font-medium border border-blue-200 dark:border-blue-900">
            Imutabilidade Ativa
          </span>
        </div>

        {submitFeedback && (
          <div
            role="alert"
            className={`mt-4 p-3.5 rounded-lg text-xs font-medium flex items-center gap-2 ${
              submitFeedback.type === 'success'
                ? 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-800 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-900'
                : 'bg-rose-50 dark:bg-rose-950/40 text-rose-800 dark:text-rose-300 border border-rose-200 dark:border-rose-900'
            }`}
          >
            <span>{submitFeedback.type === 'success' ? '✓' : '⚠️'}</span>
            <span>{submitFeedback.message}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate className="mt-5 space-y-5">
          {/* Linha 1: Identificadores */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label htmlFor="cost-product-id" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                ID do Produto (UUID) <span className="text-rose-500">*</span>
              </label>
              <input
                id="cost-product-id"
                name="productId"
                type="text"
                placeholder="Ex: 550e8400-e29b-41d4-a716-446655440000"
                value={formState.productId}
                onChange={(e) => setFormState({ ...formState, productId: e.target.value })}
                aria-invalid={!!errors.productId}
                aria-describedby={errors.productId ? 'cost-product-id-error' : undefined}
                className={`w-full text-xs px-3 py-2 rounded-lg border bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 ${
                  errors.productId
                    ? 'border-rose-500 focus:ring-rose-500'
                    : 'border-slate-300 dark:border-slate-700 focus:ring-blue-500'
                }`}
              />
              {errors.productId && (
                <p id="cost-product-id-error" role="alert" className="text-[11px] text-rose-500 mt-1 font-medium">
                  {errors.productId}
                </p>
              )}
            </div>

            <div>
              <label htmlFor="cost-variant-id" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                ID da Variante (Opcional)
              </label>
              <input
                id="cost-variant-id"
                name="variantId"
                type="text"
                placeholder="Vazio para custo do produto base"
                value={formState.variantId}
                onChange={(e) => setFormState({ ...formState, variantId: e.target.value })}
                className="w-full text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label htmlFor="cost-company-id" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                ID da Empresa / Filial (Opcional)
              </label>
              <input
                id="cost-company-id"
                name="companyId"
                type="text"
                placeholder="Vazio para custo padrão multi-empresa"
                value={formState.companyId}
                onChange={(e) => setFormState({ ...formState, companyId: e.target.value })}
                className="w-full text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          {/* Linha 2: Custos Diretos em R$ */}
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
            <div>
              <label htmlFor="cost-acquisition" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Custo de Aquisição (R$) <span className="text-rose-500">*</span>
              </label>
              <input
                id="cost-acquisition"
                name="acquisitionCost"
                type="number"
                step="0.01"
                min="0.01"
                placeholder="0.00"
                value={formState.acquisitionCost}
                onChange={(e) => setFormState({ ...formState, acquisitionCost: e.target.value })}
                aria-invalid={!!errors.acquisitionCost}
                aria-describedby={errors.acquisitionCost ? 'cost-acquisition-error' : undefined}
                className={`w-full text-xs px-3 py-2 rounded-lg border bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 ${
                  errors.acquisitionCost
                    ? 'border-rose-500 focus:ring-rose-500'
                    : 'border-slate-300 dark:border-slate-700 focus:ring-blue-500'
                }`}
              />
              {errors.acquisitionCost && (
                <p id="cost-acquisition-error" role="alert" className="text-[11px] text-rose-500 mt-1 font-medium">
                  {errors.acquisitionCost}
                </p>
              )}
            </div>

            <div>
              <label htmlFor="cost-packaging" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Embalagem & Insumos (R$)
              </label>
              <input
                id="cost-packaging"
                name="packagingCost"
                type="number"
                step="0.01"
                min="0.00"
                placeholder="0.00"
                value={formState.packagingCost}
                onChange={(e) => setFormState({ ...formState, packagingCost: e.target.value })}
                aria-invalid={!!errors.packagingCost}
                className="w-full text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              {errors.packagingCost && (
                <p role="alert" className="text-[11px] text-rose-500 mt-1 font-medium">{errors.packagingCost}</p>
              )}
            </div>

            <div>
              <label htmlFor="cost-additional" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Custos Adicionais (R$)
              </label>
              <input
                id="cost-additional"
                name="additionalCost"
                type="number"
                step="0.01"
                min="0.00"
                placeholder="0.00"
                value={formState.additionalCost}
                onChange={(e) => setFormState({ ...formState, additionalCost: e.target.value })}
                className="w-full text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              {errors.additionalCost && (
                <p role="alert" className="text-[11px] text-rose-500 mt-1 font-medium">{errors.additionalCost}</p>
              )}
            </div>

            <div>
              <label htmlFor="cost-subsidized-shipping" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Frete Subsidiado (R$)
              </label>
              <input
                id="cost-subsidized-shipping"
                name="subsidizedShippingCost"
                type="number"
                step="0.01"
                min="0.00"
                placeholder="0.00"
                value={formState.subsidizedShippingCost}
                onChange={(e) => setFormState({ ...formState, subsidizedShippingCost: e.target.value })}
                className="w-full text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              {errors.subsidizedShippingCost && (
                <p role="alert" className="text-[11px] text-rose-500 mt-1 font-medium">{errors.subsidizedShippingCost}</p>
              )}
            </div>
          </div>

          {/* Linha 3: Alíquotas Percentuais & Vigência */}
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
            <div>
              <label htmlFor="cost-tax-rate" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Imposto Estimado (%)
              </label>
              <input
                id="cost-tax-rate"
                name="taxRatePercent"
                type="number"
                step="0.01"
                min="0.00"
                max="100.00"
                placeholder="0.00"
                value={formState.taxRatePercent}
                onChange={(e) => setFormState({ ...formState, taxRatePercent: e.target.value })}
                className="w-full text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              {errors.taxRatePercent && (
                <p role="alert" className="text-[11px] text-rose-500 mt-1 font-medium">{errors.taxRatePercent}</p>
              )}
            </div>

            <div>
              <label htmlFor="cost-other-expenses" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Outras Despesas (%)
              </label>
              <input
                id="cost-other-expenses"
                name="otherExpensesPercent"
                type="number"
                step="0.01"
                min="0.00"
                max="100.00"
                placeholder="0.00"
                value={formState.otherExpensesPercent}
                onChange={(e) => setFormState({ ...formState, otherExpensesPercent: e.target.value })}
                className="w-full text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              {errors.otherExpensesPercent && (
                <p role="alert" className="text-[11px] text-rose-500 mt-1 font-medium">{errors.otherExpensesPercent}</p>
              )}
            </div>

            <div>
              <label htmlFor="cost-effective-from" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Vigência Inicial <span className="text-rose-500">*</span>
              </label>
              <input
                id="cost-effective-from"
                name="effectiveFrom"
                type="datetime-local"
                value={formState.effectiveFrom}
                onChange={(e) => setFormState({ ...formState, effectiveFrom: e.target.value })}
                aria-invalid={!!errors.effectiveFrom}
                className="w-full text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              {errors.effectiveFrom && (
                <p role="alert" className="text-[11px] text-rose-500 mt-1 font-medium">{errors.effectiveFrom}</p>
              )}
            </div>

            <div>
              <label htmlFor="cost-source-type" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Origem da Informação
              </label>
              <select
                id="cost-source-type"
                name="sourceType"
                value={formState.sourceType}
                onChange={(e) => setFormState({ ...formState, sourceType: e.target.value })}
                className="w-full text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="USER_INPUT">Entrada Manual do Usuário</option>
                <option value="ERP_SYNC">Sincronização com ERP / NF-e</option>
                <option value="SUPPLIER_INVOICE">Nota Fiscal de Fornecedor</option>
              </select>
            </div>
          </div>

          {/* Linha 4: Observações e Ação */}
          <div className="flex flex-col sm:flex-row items-end gap-4 pt-2">
            <div className="flex-1 w-full">
              <label htmlFor="cost-notes" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Observações de Auditoria (Opcional)
              </label>
              <input
                id="cost-notes"
                name="notes"
                type="text"
                placeholder="Ex: Custo renegociado lote 2026/11 com 5% de desconto"
                value={formState.notes}
                onChange={(e) => setFormState({ ...formState, notes: e.target.value })}
                className="w-full text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full sm:w-auto px-5 py-2.5 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white text-xs font-bold rounded-lg shadow transition-colors flex items-center justify-center gap-2 min-h-[44px]"
            >
              {isSubmitting ? (
                <>
                  <span className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  <span>Salvando Versão...</span>
                </>
              ) : (
                <span>Registrar Versão de Custo</span>
              )}
            </button>
          </div>
        </form>
      </section>

      {/* Histórico Visível de Versões */}
      <section
        aria-labelledby="cost-history-heading"
        className="bg-white dark:bg-slate-900 rounded-xl p-6 border border-slate-200 dark:border-slate-800 shadow-sm"
      >
        <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800">
          <div>
            <h3 id="cost-history-heading" className="text-sm font-bold text-slate-900 dark:text-white">
              Histórico de Versões do Produto
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              {formState.productId.trim()
                ? `Registros encadeados para o produto ${formState.productId}`
                : 'Informe um ID de produto acima para visualizar o histórico de vigências.'}
            </p>
          </div>
          {history.length > 0 && (
            <span className="text-xs font-semibold px-2 py-0.5 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 rounded">
              {history.length} versão(ões)
            </span>
          )}
        </div>

        {isLoadingHistory ? (
          <div className="py-8 text-center text-xs text-slate-500 dark:text-slate-400" role="status">
            Carregando histórico de custos...
          </div>
        ) : history.length === 0 ? (
          <div className="py-8 text-center text-xs text-slate-500 dark:text-slate-400" role="region">
            Nenhuma versão de custo registrada para o produto informado.
          </div>
        ) : (
          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 dark:border-slate-800 text-slate-500 dark:text-slate-400">
                  <th className="py-2.5 px-3 font-semibold">Versão</th>
                  <th className="py-2.5 px-3 font-semibold">Status</th>
                  <th className="py-2.5 px-3 font-semibold">Período de Vigência</th>
                  <th className="py-2.5 px-3 font-semibold">Aquisição</th>
                  <th className="py-2.5 px-3 font-semibold">Custos Indiretos</th>
                  <th className="py-2.5 px-3 font-semibold">Imposto (%)</th>
                  <th className="py-2.5 px-3 font-semibold">Origem</th>
                  <th className="py-2.5 px-3 font-semibold">Registrado Por</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800 text-slate-800 dark:text-slate-200">
                {history.map((item) => {
                  const isCurrent = !item.effectiveTo || new Date(item.effectiveTo) > new Date();
                  const totalDirectExtras = item.packagingCost + item.additionalCost + item.subsidizedShippingCost;

                  return (
                    <tr
                      key={item.id}
                      className={isCurrent ? 'bg-emerald-50/40 dark:bg-emerald-950/20 font-medium' : ''}
                    >
                      <td className="py-3 px-3">
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-bold bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200">
                          v{item.version}
                        </span>
                      </td>
                      <td className="py-3 px-3">
                        {isCurrent ? (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-semibold bg-emerald-100 dark:bg-emerald-900/40 text-emerald-800 dark:text-emerald-300">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                            Vigente
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-medium bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400">
                            Encerrado
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-3 text-[11px] text-slate-600 dark:text-slate-400">
                        <div>De: {new Date(item.effectiveFrom).toLocaleDateString('pt-BR')} {new Date(item.effectiveFrom).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}</div>
                        <div>Até: {item.effectiveTo ? `${new Date(item.effectiveTo).toLocaleDateString('pt-BR')} ${new Date(item.effectiveTo).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}` : 'Atualmente'}</div>
                      </td>
                      <td className="py-3 px-3 font-semibold text-slate-900 dark:text-white">
                        {item.currency} {item.acquisitionCost.toFixed(2)}
                      </td>
                      <td className="py-3 px-3 text-slate-600 dark:text-slate-400">
                        {item.currency} {totalDirectExtras.toFixed(2)}
                        <span className="text-[10px] block opacity-75">
                          (Emb: {item.packagingCost.toFixed(2)}, Add: {item.additionalCost.toFixed(2)})
                        </span>
                      </td>
                      <td className="py-3 px-3">
                        {item.taxRatePercent.toFixed(2)}%
                      </td>
                      <td className="py-3 px-3 text-[11px]">
                        <span className="px-1.5 py-0.5 rounded bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300">
                          {item.sourceType}
                        </span>
                      </td>
                      <td className="py-3 px-3 text-[11px] text-slate-500">
                        {item.createdByName || 'Sistema'}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
